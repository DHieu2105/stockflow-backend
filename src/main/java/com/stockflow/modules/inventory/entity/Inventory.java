package com.stockflow.modules.inventory.entity;

import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.warehouse.entity.Warehouse;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_inventories_warehouse_variant",
                        columnNames = {"warehouse_id", "variant_id"}
                )
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "on_hand", nullable = false)
    private Integer onHand = 0;

    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory() {
    }

    public Inventory(Warehouse warehouse, ProductVariant variant) {
        this.warehouse = warehouse;
        this.variant = variant;
        this.onHand = 0;
        this.reserved = 0;
        this.version = 0L;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.onHand == null) {
            this.onHand = 0;
        }
        if (this.reserved == null) {
            this.reserved = 0;
        }
        validateState();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        validateState();
        this.updatedAt = Instant.now();
    }

    public void validateState() {
        if (this.onHand < 0) {
            throw new IllegalStateException("On hand inventory cannot be negative");
        }
        if (this.reserved < 0) {
            throw new IllegalStateException("Reserved inventory cannot be negative");
        }
        if (this.reserved > this.onHand) {
            throw new IllegalStateException("Reserved inventory cannot exceed on hand inventory");
        }
    }

    public int getAvailable() {
        int oh = this.onHand != null ? this.onHand : 0;
        int res = this.reserved != null ? this.reserved : 0;
        return oh - res;
    }

    public Integer getAvailableQuantity() {
        return getAvailable();
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (this.onHand == null) {
            this.onHand = 0;
        }
        this.onHand += quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (quantity > getAvailable()) {
            throw new IllegalStateException("Cannot decrease stock more than available quantity");
        }
        this.onHand -= quantity;
    }

    public UUID getId() {
        return id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public Integer getOnHand() {
        return onHand;
    }

    public void setOnHand(Integer onHand) {
        this.onHand = onHand;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

