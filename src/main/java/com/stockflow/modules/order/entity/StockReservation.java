package com.stockflow.modules.order.entity;

import com.stockflow.modules.warehouse.entity.Warehouse;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockReservationStatus status = StockReservationStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockReservation() {
    }

    public StockReservation(
            Order order,
            Warehouse warehouse,
            Instant expiresAt
    ) {
        this.order = order;
        this.warehouse = warehouse;
        this.expiresAt = expiresAt;
        this.status = StockReservationStatus.ACTIVE;
    }

    public void release() {
        ensureActive();
        this.status = StockReservationStatus.RELEASED;
    }

    public void consume() {
        ensureActive();
        this.status = StockReservationStatus.CONSUMED;
    }

    public void expire() {
        ensureActive();
        this.status = StockReservationStatus.EXPIRED;
    }

    private void ensureActive() {
        if (this.status != StockReservationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active reservations can change status"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (this.status == null) {
            this.status = StockReservationStatus.ACTIVE;
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

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public StockReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
