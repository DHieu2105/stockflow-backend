package com.stockflow.modules.inventory.dto;

import com.stockflow.modules.inventory.entity.Inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(

        UUID id,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        UUID variantId,
        String sku,
        String variantName,
        Integer onHand,
        Integer reserved,
        Integer available,
        Instant updatedAt

) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getCode(),
                inventory.getWarehouse().getName(),
                inventory.getVariant().getId(),
                inventory.getVariant().getSku(),
                inventory.getVariant().getVariantName(),
                inventory.getOnHand(),
                inventory.getReserved(),
                inventory.getAvailable(),
                inventory.getUpdatedAt()
        );
    }
}

