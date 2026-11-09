package com.stockflow.modules.warehouse.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseAccessTestController {

    @GetMapping("/{warehouseId}/access-test")
    @PreAuthorize("@warehouseAuthorization.hasAccess(#warehouseId)")
    public Map<String, Object> testWarehouseAccess(
            @PathVariable UUID warehouseId
    ) {
        return Map.of(
                "warehouseId", warehouseId,
                "access", "GRANTED",
                "message", "You can access this warehouse"
        );
    }
}

