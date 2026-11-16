package com.stockflow.modules.order.entity;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    // 1. Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 2. Order information
    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfillment_warehouse_id", nullable = false)
    private Warehouse fulfillmentWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    // 3. Financial information
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // 4. Recipient snapshot
    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    // 5. Shipping address snapshot (PostgreSQL JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> shippingAddress;

    // 6. Additional information
    @Column(columnDefinition = "TEXT")
    private String note;

    // 7. Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA requires a no-argument constructor
    protected Order() {
    }

    // Initialize a new order
    public Order(
            String orderCode,
            User customer,
            Warehouse fulfillmentWarehouse,
            String recipientName,
            String recipientPhone,
            Map<String, Object> shippingAddress,
            String note
    ) {
        this.orderCode = orderCode;
        this.customer = customer;
        this.fulfillmentWarehouse = fulfillmentWarehouse;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.subtotal = BigDecimal.ZERO;
        this.shippingFee = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    // Calculate and update order amounts
    public void updateAmounts(
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal discountAmount
    ) {
        if (subtotal == null || shippingFee == null || discountAmount == null) {
            throw new IllegalArgumentException("Order amounts must not be null");
        }

        if (subtotal.signum() < 0
                || shippingFee.signum() < 0
                || discountAmount.signum() < 0) {
            throw new IllegalArgumentException("Order amounts must not be negative");
        }

        BigDecimal totalBeforeDiscount = subtotal.add(shippingFee);

        if (discountAmount.compareTo(totalBeforeDiscount) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot exceed subtotal plus shipping fee"
            );
        }

        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.totalAmount = totalBeforeDiscount.subtract(discountAmount);
    }

    // Update order status
    public void updateStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status must not be null");
        }

        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (this.status == null) {
            this.status = OrderStatus.PENDING_PAYMENT;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public User getCustomer() {
        return customer;
    }

    public Warehouse getFulfillmentWarehouse() {
        return fulfillmentWarehouse;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public Map<String, Object> getShippingAddress() {
        return shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
