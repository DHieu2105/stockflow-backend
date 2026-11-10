package com.stockflow.modules.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String address,

        @Size(max = 100)
        String province

) {
}

