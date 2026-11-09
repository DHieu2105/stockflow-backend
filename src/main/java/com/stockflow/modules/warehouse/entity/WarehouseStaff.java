package com.stockflow.modules.warehouse.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "warehouse_staff")
public class WarehouseStaff {

    @EmbeddedId
    private WarehouseStaffId id;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected WarehouseStaff() {
    }

    public WarehouseStaff(WarehouseStaffId id) {
        this.id = id;
        this.assignedAt = Instant.now();
    }

    public WarehouseStaffId getId() {
        return id;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}

