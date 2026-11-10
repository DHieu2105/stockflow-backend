package com.stockflow.modules.category.dto;

import com.stockflow.modules.category.entity.Category;
import com.stockflow.modules.category.entity.CategoryStatus;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        String slug,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParent() != null
                        ? category.getParent().getId()
                        : null,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}

