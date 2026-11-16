package com.stockflow.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockTransferRequest(

        @NotNull
        UUID sourceWarehouseId,

        @NotNull
        UUID destinationWarehouseId,

        @NotNull
        UUID variantId,

        @Positive
        int quantity,

        String note

) {
}
