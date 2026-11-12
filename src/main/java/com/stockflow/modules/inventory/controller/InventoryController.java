package com.stockflow.modules.inventory.controller;

import com.stockflow.modules.inventory.dto.InventoryResponse;
import com.stockflow.modules.inventory.dto.StockAdjustmentRequest;
import com.stockflow.modules.inventory.dto.StockInboundRequest;
import com.stockflow.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inbound")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<InventoryResponse> inboundStock(
            @Valid @RequestBody StockInboundRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        InventoryResponse response = inventoryService.inboundStock(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<InventoryResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        InventoryResponse response = inventoryService.adjustStock(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/warehouses/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<Page<InventoryResponse>> getWarehouseInventories(
            @PathVariable UUID warehouseId,
            Pageable pageable
    ) {
        Page<InventoryResponse> response = inventoryService.getWarehouseInventories(warehouseId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouses/{warehouseId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable UUID warehouseId,
            @PathVariable UUID variantId
    ) {
        InventoryResponse response = inventoryService.getInventory(warehouseId, variantId);
        return ResponseEntity.ok(response);
    }
}

