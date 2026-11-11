package com.stockflow.modules.product.dto;

import com.stockflow.modules.product.entity.ProductStatus;
import com.stockflow.modules.product.entity.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductVariantResponse(

        UUID id,
        UUID productId,
        String sku,
        String variantName,
        BigDecimal price,
        Map<String, Object> attributes,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getVariantName(),
                variant.getPrice(),
                variant.getAttributes(),
                variant.getStatus(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}

