package com.stockflow.modules.warehouse.controller;

import com.stockflow.modules.warehouse.dto.WarehouseRequest;
import com.stockflow.modules.warehouse.dto.WarehouseResponse;
import com.stockflow.modules.warehouse.dto.WarehouseUpdateRequest;
import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.service.WarehouseService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_STAFF')")
    public List<WarehouseResponse> getAllWarehouses(
            Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        UUID userId = (UUID) authentication.getPrincipal();

        List<Warehouse> warehouses = isAdmin
                ? warehouseService.getAllWarehouses()
                : warehouseService.getWarehousesForUser(userId);

        return warehouses.stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize("@warehouseAuthorization.hasAccess(#warehouseId)")
    public WarehouseResponse getWarehouse(
            @PathVariable UUID warehouseId
    ) {
        return WarehouseResponse.from(
                warehouseService.getWarehouseById(warehouseId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse createWarehouse(
            @Valid @RequestBody WarehouseRequest request
    ) {
        return WarehouseResponse.from(
                warehouseService.createWarehouse(
                        request.code(),
                        request.name(),
                        request.address(),
                        request.province()
                )
        );
    }

    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse updateWarehouse(
            @PathVariable UUID warehouseId,
            @Valid @RequestBody WarehouseUpdateRequest request
    ) {
        return WarehouseResponse.from(
                warehouseService.updateWarehouse(
                        warehouseId,
                        request.name(),
                        request.address(),
                        request.province()
                )
        );
    }

    @PatchMapping("/{warehouseId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse deactivateWarehouse(
            @PathVariable UUID warehouseId
    ) {
        return WarehouseResponse.from(
                warehouseService.deactivateWarehouse(warehouseId)
        );
    }
}

