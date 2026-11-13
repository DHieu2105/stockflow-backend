package com.stockflow.modules.inventory.dto;

import com.stockflow.modules.inventory.entity.StockMovement;
import com.stockflow.modules.inventory.entity.StockMovementType;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID inventoryId,
        UUID warehouseId,
        String warehouseCode,
        UUID variantId,
        String sku,
        StockMovementType movementType,
        Integer quantity,
        Integer quantityBefore,
        Integer quantityAfter,
        String referenceType,
        UUID referenceId,
        String note,
        UUID createdBy,
        String createdByEmail,
        Instant createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getInventory().getId(),
                movement.getInventory().getWarehouse().getId(),
                movement.getInventory().getWarehouse().getCode(),
                movement.getInventory().getVariant().getId(),
                movement.getInventory().getVariant().getSku(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getNote(),
                movement.getCreatedBy() != null ? movement.getCreatedBy().getId() : null,
                movement.getCreatedBy() != null ? movement.getCreatedBy().getEmail() : null,
                movement.getCreatedAt()
        );
    }
}

