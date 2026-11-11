package com.stockflow.modules.product.repository;

import com.stockflow.modules.product.entity.Product;
import com.stockflow.modules.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(UUID id);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory_Id(Long categoryId);

    List<Product> findByCategory_IdAndStatus(
            Long categoryId,
            ProductStatus status
    );
}

