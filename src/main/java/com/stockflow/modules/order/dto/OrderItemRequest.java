package com.stockflow.modules.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(

        @NotNull(message = "Variant ID không được để trống")
        UUID variantId,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 999, message = "Số lượng tối đa là 999")
        Integer quantity

) {
}
