package com.stockflow.modules.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(

        @NotNull(message = "Warehouse ID is required")
        UUID warehouseId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotNull(message = "Actual quantity is required")
        @Min(value = 0, message = "Actual quantity must be greater than or equal to 0")
        Integer actualQuantity,

        @Size(max = 50, message = "Reference type must not exceed 50 characters")
        String referenceType,

        UUID referenceId,

        String note

) {}
