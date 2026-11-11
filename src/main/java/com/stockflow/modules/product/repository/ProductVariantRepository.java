package com.stockflow.modules.product.repository;

import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<ProductVariant> findByProduct_Id(UUID productId);

    List<ProductVariant> findByProduct_IdAndStatus(
            UUID productId,
            ProductStatus status
    );
}

