package com.stockflow.modules.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockInboundRequest(

        @NotNull(message = "Warehouse ID is required")
        UUID warehouseId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @Size(max = 50, message = "Reference type must not exceed 50 characters")
        String referenceType,

        UUID referenceId,

        String note

) {}
