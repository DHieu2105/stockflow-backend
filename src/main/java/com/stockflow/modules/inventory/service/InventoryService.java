package com.stockflow.modules.inventory.service;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.identity.repository.UserRepository;
import com.stockflow.modules.inventory.dto.InventoryResponse;
import com.stockflow.modules.inventory.dto.StockAdjustmentRequest;
import com.stockflow.modules.inventory.dto.StockInboundRequest;
import com.stockflow.modules.inventory.dto.StockMovementResponse;
import com.stockflow.modules.inventory.dto.StockOutboundRequest;
import com.stockflow.modules.inventory.dto.StockTransferRequest;
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

import java.util.List;
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

    // ==================== STOCK OUTBOUND ====================

    public InventoryResponse outboundStock(
            StockOutboundRequest request,
            UUID userId
    ) {
        return outboundStock(request.warehouseId(), request, userId);
    }

    public InventoryResponse outboundStock(
            UUID warehouseId,
            StockOutboundRequest request,
            UUID userId
    ) {
        // Kiểm tra quyền truy cập kho
        warehouseAuthorizationService.checkAccess(warehouseId);

        Warehouse warehouse = getActiveWarehouse(warehouseId);
        getActiveVariant(request.variantId());

        if (request.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Outbound quantity must be greater than zero"
            );
        }

        // Khóa bản ghi tồn kho để chống xuất kho đồng thời
        Inventory inventory = inventoryRepository
                .findByWarehouseAndVariantForUpdate(
                        warehouseId,
                        request.variantId()
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Inventory not found for this warehouse and variant"
                        )
                );

        int available = inventory.getAvailable();

        // Không cho xuất vượt số lượng khả dụng
        if (request.quantity() > available) {
            throw new IllegalStateException(
                    String.format(
                            "Insufficient available stock. Available: %d, requested: %d",
                            available,
                            request.quantity()
                    )
            );
        }

        int quantityBefore = inventory.getOnHand();
        int quantityAfter = quantityBefore - request.quantity();

        // Chỉ giảm onHand, không thay đổi reserved
        inventory.setOnHand(quantityAfter);
        inventory = inventoryRepository.save(inventory);

        // Xác định người thực hiện
        User user = (userId != null)
                ? userRepository.findById(userId).orElse(null)
                : null;

        // Ghi lịch sử xuất kho
        StockMovement movement = new StockMovement(
                inventory,
                StockMovementType.OUTBOUND,
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

    // ==================== STOCK TRANSFER ====================

    public InventoryResponse transferStock(
            StockTransferRequest request,
            UUID userId
    ) {
        UUID sourceWarehouseId = request.sourceWarehouseId();
        UUID destinationWarehouseId = request.destinationWarehouseId();

        // 1. Không cho chuyển cùng một kho
        if (sourceWarehouseId.equals(destinationWarehouseId)) {
            throw new IllegalArgumentException(
                    "Source and destination warehouses must be different"
            );
        }

        // 2. Kiểm tra quyền truy cập cả hai kho
        warehouseAuthorizationService.checkAccess(sourceWarehouseId);
        warehouseAuthorizationService.checkAccess(destinationWarehouseId);

        // 3. Khóa hai Warehouse theo cùng một thứ tự UUID
        List<UUID> warehouseIds = List.of(
                sourceWarehouseId,
                destinationWarehouseId
        ).stream().sorted().toList();

        for (UUID warehouseId : warehouseIds) {
            Warehouse lockedWarehouse = warehouseRepository
                    .findByIdForUpdate(warehouseId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Warehouse not found")
                    );

            if (lockedWarehouse.getStatus() != WarehouseStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Cannot transfer stock to or from an inactive warehouse"
                );
            }
        }

        // 4. Kiểm tra variant
        ProductVariant variant = getActiveVariant(request.variantId());

        // 5. Khóa Inventory của cả hai kho
        List<Inventory> lockedInventories =
                inventoryRepository.findByWarehousesAndVariantForUpdate(
                        warehouseIds,
                        request.variantId()
                );

        Inventory sourceInventory = lockedInventories.stream()
                .filter(i -> i.getWarehouse().getId().equals(sourceWarehouseId))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Source inventory not found"
                        )
                );

        // 6. Tìm hoặc tạo Inventory tại kho đích
        Inventory destinationInventory = lockedInventories.stream()
                .filter(i -> i.getWarehouse().getId().equals(destinationWarehouseId))
                .findFirst()
                .orElseGet(() -> {
                    Warehouse destinationWarehouse = warehouseRepository
                            .findById(destinationWarehouseId)
                            .orElseThrow(() ->
                                    new EntityNotFoundException("Destination warehouse not found")
                            );

                    return inventoryRepository.save(
                            new Inventory(destinationWarehouse, variant)
                    );
                });

        // 7. Kiểm tra số lượng chuyển
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer quantity must be greater than zero"
            );
        }

        int available = sourceInventory.getAvailable();

        if (request.quantity() > available) {
            throw new IllegalStateException(
                    String.format(
                            "Insufficient available stock. Available: %d, requested: %d",
                            available,
                            request.quantity()
                    )
            );
        }

        // 8. Lưu số lượng trước và sau
        int sourceBefore = sourceInventory.getOnHand();
        int sourceAfter = sourceBefore - request.quantity();

        int destinationBefore = destinationInventory.getOnHand();
        int destinationAfter = destinationBefore + request.quantity();

        // 9. Cập nhật tồn kho hai kho
        sourceInventory.decreaseStock(request.quantity());
        destinationInventory.increaseStock(request.quantity());

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destinationInventory);

        // 10. Lấy người thực hiện
        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        // 11. Tạo reference ID chung cho hai movement
        UUID transferReferenceId = UUID.randomUUID();

        // 12. Ghi TRANSFER_OUT tại kho nguồn
        StockMovement transferOut = new StockMovement(
                sourceInventory,
                StockMovementType.TRANSFER_OUT,
                request.quantity(),
                sourceBefore,
                sourceAfter,
                "STOCK_TRANSFER",
                transferReferenceId,
                request.note(),
                user
        );

        // 13. Ghi TRANSFER_IN tại kho đích
        StockMovement transferIn = new StockMovement(
                destinationInventory,
                StockMovementType.TRANSFER_IN,
                request.quantity(),
                destinationBefore,
                destinationAfter,
                "STOCK_TRANSFER",
                transferReferenceId,
                request.note(),
                user
        );

        stockMovementRepository.saveAll(
                List.of(transferOut, transferIn)
        );

        // 14. Trả về tồn kho kho đích
        return InventoryResponse.from(destinationInventory);
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
