package com.stockflow.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantUpdateRequest(

        @NotBlank
        @Size(max = 100)
        String sku,

        @NotBlank
        @Size(max = 150)
        String variantName,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal price,

        Map<String, Object> attributes

) {}

