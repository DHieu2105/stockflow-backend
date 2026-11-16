package com.stockflow.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockOutboundRequest(

        @NotNull
        UUID warehouseId,

        @NotNull
        UUID variantId,

        @Positive
        int quantity,

        String referenceType,

        UUID referenceId,

        String note

) {
}
