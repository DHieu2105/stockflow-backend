package com.stockflow.modules.warehouse.repository;

import com.stockflow.modules.warehouse.entity.WarehouseStaff;
import com.stockflow.modules.warehouse.entity.WarehouseStaffId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WarehouseStaffRepository
        extends JpaRepository<WarehouseStaff, WarehouseStaffId> {

    boolean existsById_WarehouseIdAndId_UserId(
            UUID warehouseId,
            UUID userId
    );

    List<WarehouseStaff> findById_UserId(UUID userId);
}

