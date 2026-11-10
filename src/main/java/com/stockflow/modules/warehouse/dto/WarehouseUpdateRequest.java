package com.stockflow.modules.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 150, message = "Warehouse name cannot exceed 150 characters")
        String name,

        @NotBlank(message = "Warehouse address is required")
        @Size(max = 500, message = "Warehouse address cannot exceed 500 characters")
        String address,

        @NotBlank(message = "Province is required")
        @Size(max = 100, message = "Province cannot exceed 100 characters")
        String province

) {
}

