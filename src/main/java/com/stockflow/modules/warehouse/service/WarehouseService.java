package com.stockflow.modules.warehouse.service;

import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.entity.WarehouseStaff;
import com.stockflow.modules.warehouse.entity.WarehouseStatus;
import com.stockflow.modules.warehouse.repository.WarehouseRepository;
import com.stockflow.modules.warehouse.repository.WarehouseStaffRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseStaffRepository warehouseStaffRepository;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            WarehouseStaffRepository warehouseStaffRepository
    ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseStaffRepository = warehouseStaffRepository;
    }

    // Lấy danh sách tất cả kho
    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    // Lấy danh sách kho cho user được phân công
    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesForUser(UUID userId) {
        List<WarehouseStaff> assignments =
                warehouseStaffRepository.findById_UserId(userId);

        List<UUID> warehouseIds = assignments.stream()
                .map(assignment -> assignment.getId().getWarehouseId())
                .toList();

        return warehouseRepository.findAllById(warehouseIds);
    }

    // Lấy thông tin một kho
    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Warehouse not found")
                );
    }

    // Tạo kho mới
    @Transactional
    public Warehouse createWarehouse(
            String code,
            String name,
            String address,
            String province
    ) {
        if (warehouseRepository.existsByCode(code)) {
            throw new IllegalArgumentException(
                    "Warehouse code already exists"
            );
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setProvince(province);
        warehouse.setStatus(WarehouseStatus.ACTIVE);

        return warehouseRepository.save(warehouse);
    }

    // Cập nhật thông tin kho
    @Transactional
    public Warehouse updateWarehouse(
            UUID warehouseId,
            String name,
            String address,
            String province
    ) {
        Warehouse warehouse = getWarehouseById(warehouseId);

        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setProvince(province);

        return warehouseRepository.save(warehouse);
    }

    // Ngừng hoạt động kho
    @Transactional
    public Warehouse deactivateWarehouse(UUID warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);

        if (warehouse.getStatus() == WarehouseStatus.INACTIVE) {
            throw new IllegalStateException("Warehouse is already inactive");
        }

        if (warehouseRepository.countActiveInventory(warehouseId) > 0) {
            throw new IllegalStateException(
                    "Cannot deactivate warehouse that still has active inventory"
            );
        }

        if (warehouseRepository.countPendingOrders(warehouseId) > 0) {
            throw new IllegalStateException(
                    "Cannot deactivate warehouse with orders currently in progress"
            );
        }

        warehouse.setStatus(WarehouseStatus.INACTIVE);

        return warehouseRepository.save(warehouse);
    }
}

