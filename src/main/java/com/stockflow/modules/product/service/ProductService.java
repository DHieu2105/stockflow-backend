package com.stockflow.modules.product.service;

import com.stockflow.modules.category.entity.Category;
import com.stockflow.modules.category.entity.CategoryStatus;
import com.stockflow.modules.category.repository.CategoryRepository;
import com.stockflow.modules.product.entity.Product;
import com.stockflow.modules.product.entity.ProductStatus;
import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.repository.ProductRepository;
import com.stockflow.modules.product.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
    }

    // ==================== PRODUCT ====================

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Product getProductById(UUID productId) {
        return productRepository.findWithCategoryById(productId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found"));
    }

    @Transactional(readOnly = true)
    public Product getActiveProductById(UUID productId) {
        Product product = getProductById(productId);

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new EntityNotFoundException("Product not found");
        }

        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        Category category = getActiveCategory(categoryId);

        return productRepository.findByCategory_IdAndStatus(
                category.getId(),
                ProductStatus.ACTIVE
        );
    }

    public Product createProduct(
            String name,
            String slug,
            String description,
            String brand,
            Long categoryId
    ) {
        if (productRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                    "Product slug already exists"
            );
        }

        Category category = getActiveCategory(categoryId);

        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setBrand(brand);
        product.setCategory(category);
        product.setStatus(ProductStatus.DRAFT);

        return productRepository.save(product);
    }

    public Product updateProduct(
            UUID productId,
            String name,
            String slug,
            String description,
            String brand,
            Long categoryId
    ) {
        Product product = getProductById(productId);

        if (productRepository.existsBySlugAndIdNot(slug, productId)) {
            throw new IllegalArgumentException(
                    "Product slug already exists"
            );
        }

        Category category = getActiveCategory(categoryId);

        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setBrand(brand);
        product.setCategory(category);

        return productRepository.save(product);
    }

    public Product activateProduct(UUID productId) {
        Product product = getProductById(productId);

        if (product.getStatus() == ProductStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Product is already active"
            );
        }

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive product cannot be activated"
            );
        }

        List<ProductVariant> activeVariants =
                variantRepository.findByProduct_IdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                );

        if (activeVariants.isEmpty()) {
            throw new IllegalStateException(
                    "Product must have at least one active variant"
            );
        }

        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    public Product deactivateProduct(UUID productId) {
        Product product = getProductById(productId);

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Product is already inactive"
            );
        }

        product.setStatus(ProductStatus.INACTIVE);
        return productRepository.save(product);
    }

    // ==================== PRODUCT VARIANT ====================

    @Transactional(readOnly = true)
    public List<ProductVariant> getProductVariants(UUID productId) {
        getProductById(productId);

        return variantRepository.findByProduct_Id(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> getActiveProductVariants(UUID productId) {
        getActiveProductById(productId);

        return variantRepository.findByProduct_IdAndStatus(
                productId,
                ProductStatus.ACTIVE
        );
    }

    public ProductVariant createVariant(
            UUID productId,
            String sku,
            String variantName,
            BigDecimal price,
            Map<String, Object> attributes
    ) {
        Product product = getProductById(productId);

        if (variantRepository.existsBySku(sku)) {
            throw new IllegalArgumentException(
                    "Product SKU already exists"
            );
        }

        validatePrice(price);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setVariantName(variantName);
        variant.setPrice(price);
        variant.setAttributes(attributes);
        variant.setStatus(ProductStatus.ACTIVE);

        return variantRepository.save(variant);
    }

    public ProductVariant updateVariant(
            UUID variantId,
            String sku,
            String variantName,
            BigDecimal price,
            Map<String, Object> attributes
    ) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product variant not found"));

        if (variantRepository.existsBySkuAndIdNot(sku, variantId)) {
            throw new IllegalArgumentException(
                    "Product SKU already exists"
            );
        }

        validatePrice(price);

        variant.setSku(sku);
        variant.setVariantName(variantName);
        variant.setPrice(price);
        variant.setAttributes(attributes);

        return variantRepository.save(variant);
    }

    public ProductVariant deactivateVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product variant not found"));

        if (variant.getStatus() == ProductStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Product variant is already inactive"
            );
        }

        variant.setStatus(ProductStatus.INACTIVE);
        return variantRepository.save(variant);
    }

    // ==================== PRIVATE HELPERS ====================

    private Category getActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Category not found"));

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot use an inactive category"
            );
        }

        return category;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Product price must be greater than or equal to zero"
            );
        }
    }
}

