package com.stockflow.modules.category.service;

import com.stockflow.modules.category.entity.Category;
import com.stockflow.modules.category.entity.CategoryStatus;
import com.stockflow.modules.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Category not found with id: " + id
                        )
                );
    }

    @Transactional(readOnly = true)
    public Category getActiveCategoryById(Long id) {
        Category category = getCategoryById(id);

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new EntityNotFoundException(
                    "Category not found with id: " + id
            );
        }

        return category;
    }

    public Category createCategory(
            String name,
            String slug,
            String description,
            Long parentId
    ) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                    "Category slug already exists"
            );
        }

        Category parent = resolveParent(parentId, null);

        Category category = new Category(
                name,
                slug,
                description,
                parent
        );

        return categoryRepository.save(category);
    }

    public Category updateCategory(
            Long id,
            String name,
            String slug,
            String description,
            Long parentId
    ) {
        Category category = getCategoryById(id);

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException(
                    "Category slug already exists"
            );
        }

        Category parent = resolveParent(parentId, id);

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setParent(parent);

        return categoryRepository.save(category);
    }

    public Category deactivateCategory(Long id) {
        Category category = getCategoryById(id);

        if (category.getStatus() == CategoryStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Category is already inactive"
            );
        }

        boolean hasActiveChildren =
                categoryRepository.existsByParent_IdAndStatus(
                        id,
                        CategoryStatus.ACTIVE
                );

        if (hasActiveChildren) {
            throw new IllegalStateException(
                    "Cannot deactivate category with active child categories"
            );
        }

        category.setStatus(CategoryStatus.INACTIVE);

        return categoryRepository.save(category);
    }

    private Category resolveParent(
            Long parentId,
            Long currentCategoryId
    ) {
        if (parentId == null) {
            return null;
        }

        if (currentCategoryId != null
                && currentCategoryId.equals(parentId)) {
            throw new IllegalArgumentException(
                    "Category cannot be its own parent"
            );
        }

        Category parent = getCategoryById(parentId);

        if (parent.getStatus() != CategoryStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Parent category is inactive"
            );
        }

        // Ngăn việc tạo vòng lặp trong cây danh mục.
        Category ancestor = parent;

        while (ancestor != null) {
            if (currentCategoryId != null
                    && currentCategoryId.equals(ancestor.getId())) {
                throw new IllegalArgumentException(
                        "Cannot move category under its own descendant"
                );
            }

            ancestor = ancestor.getParent();
        }

        return parent;
    }
}

