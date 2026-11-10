package com.stockflow.modules.category.repository;

import com.stockflow.modules.category.entity.Category;
import com.stockflow.modules.category.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findByStatus(CategoryStatus status);

    List<Category> findByParent_Id(Long parentId);

    List<Category> findByParentIsNull();

    boolean existsByParent_IdAndStatus(
            Long parentId,
            CategoryStatus status
    );
}

