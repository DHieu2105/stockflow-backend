package com.stockflow.modules.order.entity;

import com.stockflow.modules.inventory.entity.Inventory;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "stock_reservation_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_reservation_order_item",
            columnNames = {"reservation_id", "order_item_id"}
        )
    }
)
public class StockReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private StockReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockReservationItemStatus status =
            StockReservationItemStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockReservationItem() {
    }

    public StockReservationItem(
            StockReservation reservation,
            OrderItem orderItem,
            Inventory inventory,
            Integer quantity
    ) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        this.reservation = reservation;
        this.orderItem = orderItem;
        this.inventory = inventory;
        this.quantity = quantity;
        this.status = StockReservationItemStatus.ACTIVE;
    }

    public void release() {
        ensureActive();
        this.status = StockReservationItemStatus.RELEASED;
    }

    public void consume() {
        ensureActive();
        this.status = StockReservationItemStatus.CONSUMED;
    }

    private void ensureActive() {
        if (this.status != StockReservationItemStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active reservation items can change status"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.status == null) {
            this.status = StockReservationItemStatus.ACTIVE;
        }
    }

    public UUID getId() {
        return id;
    }

    public StockReservation getReservation() {
        return reservation;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public StockReservationItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
