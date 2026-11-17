package com.stockflow.modules.order.service;

import com.stockflow.modules.inventory.entity.Inventory;
import com.stockflow.modules.inventory.repository.InventoryRepository;
import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.entity.WarehouseStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WarehouseSelectionService {

    private final InventoryRepository inventoryRepository;

    public WarehouseSelectionService(
            InventoryRepository inventoryRepository
    ) {
        this.inventoryRepository = inventoryRepository;
    }

    public Warehouse selectWarehouse(
            Map<UUID, Integer> requestedItems
    ) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Đơn hàng phải có ít nhất một sản phẩm"
            );
        }

        Collection<UUID> variantIds = requestedItems.keySet();

        var inventories = inventoryRepository
                .findByWarehouse_StatusAndVariant_IdIn(
                        WarehouseStatus.ACTIVE,
                        variantIds
                );

        Map<UUID, Map<UUID, Integer>> stockByWarehouse =
                new HashMap<>();

        Map<UUID, Warehouse> warehouses = new HashMap<>();

        for (Inventory inventory : inventories) {
            UUID warehouseId = inventory.getWarehouse().getId();
            UUID variantId = inventory.getVariant().getId();

            warehouses.putIfAbsent(
                    warehouseId,
                    inventory.getWarehouse()
            );

            stockByWarehouse
                    .computeIfAbsent(
                            warehouseId,
                            ignored -> new HashMap<>()
                    )
                    .put(variantId, inventory.getAvailable());
        }

        return warehouses.entrySet()
                .stream()
                .filter(entry -> {
                    UUID warehouseId = entry.getKey();

                    Map<UUID, Integer> stock =
                            stockByWarehouse.get(warehouseId);

                    return requestedItems.entrySet()
                            .stream()
                            .allMatch(item ->
                                    stock.getOrDefault(
                                            item.getKey(), 0
                                    ) >= item.getValue()
                            );
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Không có kho nào đủ tồn kho cho toàn bộ đơn hàng"
                ));
    }
}
