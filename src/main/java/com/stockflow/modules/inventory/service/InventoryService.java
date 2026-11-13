package com.stockflow.modules.inventory.service;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.identity.repository.UserRepository;
import com.stockflow.modules.inventory.dto.InventoryResponse;
import com.stockflow.modules.inventory.dto.StockAdjustmentRequest;
import com.stockflow.modules.inventory.dto.StockInboundRequest;
import com.stockflow.modules.inventory.dto.StockMovementResponse;
import com.stockflow.modules.inventory.entity.Inventory;
import com.stockflow.modules.inventory.entity.StockMovement;
import com.stockflow.modules.inventory.entity.StockMovementType;
import com.stockflow.modules.inventory.repository.InventoryRepository;
import com.stockflow.modules.inventory.repository.StockMovementRepository;
import com.stockflow.modules.product.entity.ProductStatus;
import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.repository.ProductVariantRepository;
import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.entity.WarehouseStatus;
import com.stockflow.modules.warehouse.repository.WarehouseRepository;
import com.stockflow.modules.warehouse.service.WarehouseAuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final WarehouseAuthorizationService warehouseAuthorizationService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository,
            WarehouseRepository warehouseRepository,
            ProductVariantRepository variantRepository,
            UserRepository userRepository,
            WarehouseAuthorizationService warehouseAuthorizationService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.warehouseAuthorizationService = warehouseAuthorizationService;
    }

    // ==================== QUERIES ====================

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID warehouseId, UUID variantId) {
        // Kiểm tra quyền truy cập kho
        warehouseAuthorizationService.checkAccess(warehouseId);

        Inventory inventory = inventoryRepository.findByWarehouse_IdAndVariant_Id(warehouseId, variantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Inventory not found for this warehouse and variant"));

        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getWarehouseInventories(UUID warehouseId, Pageable pageable) {
        // Kiểm tra quyền truy cập kho
        warehouseAuthorizationService.checkAccess(warehouseId);

        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        return inventoryRepository.findByWarehouse_Id(warehouseId, pageable)
                .map(InventoryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getWarehouseMovements(
            UUID warehouseId,
            Pageable pageable
    ) {
        warehouseAuthorizationService.checkAccess(warehouseId);

        return stockMovementRepository
                .findByWarehouse_Id(warehouseId, pageable)
                .map(StockMovementResponse::from);
    }

    // ==================== STOCK INBOUND ====================

    public InventoryResponse inboundStock(StockInboundRequest request, UUID userId) {
        return inboundStock(request.warehouseId(), request, userId);
    }

    public InventoryResponse inboundStock(
            UUID warehouseId,
            StockInboundRequest request,
            UUID userId
    ) {
        // Kiểm tra quyền truy cập kho
        warehouseAuthorizationService.checkAccess(warehouseId);

        Warehouse warehouse = getActiveWarehouse(warehouseId);
        ProductVariant variant = getActiveVariant(request.variantId());

        // Khóa bi quan bản ghi tồn kho để chống race condition
        Inventory inventory = inventoryRepository
                .findByWarehouseAndVariantForUpdate(warehouseId, request.variantId())
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory(warehouse, variant);
                    return inventoryRepository.save(newInventory);
                });

        int quantityBefore = inventory.getOnHand();
        int quantityAfter = quantityBefore + request.quantity();

        // Tăng tồn kho
        inventory.increaseStock(request.quantity());
        inventory = inventoryRepository.save(inventory);

        // Ghi lại lịch sử biến động kho
        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;

        StockMovement movement = new StockMovement(
                inventory,
                StockMovementType.INBOUND,
                request.quantity(),
                quantityBefore,
                quantityAfter,
                request.referenceType(),
                request.referenceId(),
                request.note(),
                user
        );
        stockMovementRepository.save(movement);

        return InventoryResponse.from(inventory);
    }

    // ==================== STOCK ADJUSTMENT ====================

    public InventoryResponse adjustStock(StockAdjustmentRequest request, UUID userId) {
        return adjustStock(request.warehouseId(), request, userId);
    }

    public InventoryResponse adjustStock(
            UUID warehouseId,
            StockAdjustmentRequest request,
            UUID userId
    ) {
        // Kiểm tra quyền truy cập kho
        warehouseAuthorizationService.checkAccess(warehouseId);

        Warehouse warehouse = getActiveWarehouse(warehouseId);
        ProductVariant variant = getActiveVariant(request.variantId());

        // Khóa bi quan bản ghi tồn kho
        Inventory inventory = inventoryRepository
                .findByWarehouseAndVariantForUpdate(warehouseId, request.variantId())
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory(warehouse, variant);
                    return inventoryRepository.save(newInventory);
                });

        int actualQuantity = request.actualQuantity();

        // Ràng buộc bất biến: Số lượng kiểm kê thực tế không được nhỏ hơn số lượng đã reserved
        if (actualQuantity < inventory.getReserved()) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot adjust on hand stock (%d) below reserved stock (%d)",
                            actualQuantity,
                            inventory.getReserved()
                    )
            );
        }

        int quantityBefore = inventory.getOnHand();
        int quantityAfter = actualQuantity;
        int difference = Math.abs(quantityAfter - quantityBefore);

        if (difference == 0) {
            throw new IllegalArgumentException(
                    "Actual quantity is identical to current on hand stock, no adjustment needed"
            );
        }

        // Cập nhật số lượng thực tế
        inventory.setOnHand(quantityAfter);
        inventory = inventoryRepository.save(inventory);

        // Ghi lịch sử biến động
        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;

        StockMovement movement = new StockMovement(
                inventory,
                StockMovementType.ADJUSTMENT,
                difference,
                quantityBefore,
                quantityAfter,
                request.referenceType(),
                request.referenceId(),
                request.note(),
                user
        );
        stockMovementRepository.save(movement);

        return InventoryResponse.from(inventory);
    }

    // ==================== PRIVATE HELPERS ====================

    private Warehouse getActiveWarehouse(UUID warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        if (warehouse.getStatus() != WarehouseStatus.ACTIVE) {
            throw new IllegalStateException("Cannot perform inventory operations on an inactive warehouse");
        }

        return warehouse;
    }

    private ProductVariant getActiveVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found"));

        if (variant.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Cannot perform inventory operations on an inactive product variant");
        }

        return variant;
    }
}
