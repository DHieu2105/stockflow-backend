package com.stockflow.modules.order.dto;

import com.stockflow.modules.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderCode,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String recipientName,
        String recipientPhone,
        Map<String, Object> shippingAddress,
        String note,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
