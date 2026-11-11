package com.stockflow.modules.product.dto;

import com.stockflow.modules.product.entity.Product;
import com.stockflow.modules.product.entity.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(

        UUID id,
        Long categoryId,
        String categoryName,
        String name,
        String slug,
        String description,
        String brand,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getBrand(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

