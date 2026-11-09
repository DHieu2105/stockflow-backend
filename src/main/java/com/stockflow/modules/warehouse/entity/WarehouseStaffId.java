package com.stockflow.modules.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class WarehouseStaffId implements Serializable {

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "user_id")
    private UUID userId;

    protected WarehouseStaffId() {
    }

    public WarehouseStaffId(UUID warehouseId, UUID userId) {
        this.warehouseId = warehouseId;
        this.userId = userId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarehouseStaffId that)) return false;

        return Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, userId);
    }
}

