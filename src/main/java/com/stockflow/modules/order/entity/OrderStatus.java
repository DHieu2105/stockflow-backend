package com.stockflow.modules.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    DELIVERY_FAILED,
    RETURNED
}
