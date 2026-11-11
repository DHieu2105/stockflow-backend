package com.stockflow.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 220)
        String slug,

        @Size(max = 10000)
        String description,

        @Size(max = 100)
        String brand,

        @NotNull
        Long categoryId

) {}

