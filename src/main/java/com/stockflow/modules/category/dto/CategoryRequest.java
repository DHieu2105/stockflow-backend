package com.stockflow.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 150, message = "Category name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Category slug is required")
        @Size(max = 180, message = "Category slug must not exceed 180 characters")
        String slug,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        Long parentId
) {
}

