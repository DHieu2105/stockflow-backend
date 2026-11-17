package com.stockflow.modules.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID variantId,
        String sku,
        String productName,
        String variantName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
