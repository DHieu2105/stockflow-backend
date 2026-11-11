package com.stockflow.modules.category.controller;

import com.stockflow.modules.category.dto.CategoryRequest;
import com.stockflow.modules.category.dto.CategoryResponse;
import com.stockflow.modules.category.entity.Category;
import com.stockflow.modules.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getActiveCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getActiveCategoryById(id);

        return CategoryResponse.from(category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryRequest request
    ) {
        Category category = categoryService.createCategory(
                request.name(),
                request.slug(),
                request.description(),
                request.parentId()
        );

        return CategoryResponse.from(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        Category category = categoryService.updateCategory(
                id,
                request.name(),
                request.slug(),
                request.description(),
                request.parentId()
        );

        return CategoryResponse.from(category);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse deactivateCategory(@PathVariable Long id) {
        Category category = categoryService.deactivateCategory(id);

        return CategoryResponse.from(category);
    }
}

