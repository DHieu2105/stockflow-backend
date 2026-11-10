package com.stockflow.modules.warehouse.dto;

import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.entity.WarehouseStatus;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String code,
        String name,
        String address,
        String province,
        WarehouseStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getProvince(),
                warehouse.getStatus(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}

