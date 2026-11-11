package com.stockflow.modules.product.controller;

import com.stockflow.modules.product.dto.*;
import com.stockflow.modules.product.entity.Product;
import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ==================== PRODUCT ====================

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllActiveProducts()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{productId}")
    public ProductResponse getProductById(
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        boolean canViewAll = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        Set.of("ROLE_ADMIN", "ROLE_SALES_STAFF")
                                .contains(authority.getAuthority())
                );

        Product product = canViewAll
                ? productService.getProductById(productId)
                : productService.getActiveProductById(productId);

        return ProductResponse.from(product);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId
    ) {
        return productService.getProductsByCategory(categoryId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        Product product = productService.createProduct(
                request.name(),
                request.slug(),
                request.description(),
                request.brand(),
                request.categoryId()
        );

        return ProductResponse.from(product);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ProductResponse updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        Product product = productService.updateProduct(
                productId,
                request.name(),
                request.slug(),
                request.description(),
                request.brand(),
                request.categoryId()
        );

        return ProductResponse.from(product);
    }

    @PatchMapping("/{productId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse activateProduct(
            @PathVariable UUID productId
    ) {
        return ProductResponse.from(
                productService.activateProduct(productId)
        );
    }

    @PatchMapping("/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse deactivateProduct(
            @PathVariable UUID productId
    ) {
        return ProductResponse.from(
                productService.deactivateProduct(productId)
        );
    }

    // ==================== PRODUCT VARIANTS ====================

    @GetMapping("/{productId}/variants")
    public List<ProductVariantResponse> getProductVariants(
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        boolean canViewAll = canViewUnpublished(authentication);

        List<ProductVariant> variants = canViewAll
                ? productService.getProductVariants(productId)
                : productService.getActiveProductVariants(productId);

        return variants.stream()
                .map(ProductVariantResponse::from)
                .toList();
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ProductVariantResponse createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariant variant = productService.createVariant(
                productId,
                request.sku(),
                request.variantName(),
                request.price(),
                request.attributes()
        );

        return ProductVariantResponse.from(variant);
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ProductVariantResponse updateVariant(
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantUpdateRequest request
    ) {
        ProductVariant variant = productService.updateVariant(
                variantId,
                request.sku(),
                request.variantName(),
                request.price(),
                request.attributes()
        );

        return ProductVariantResponse.from(variant);
    }

    @PatchMapping("/variants/{variantId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductVariantResponse deactivateVariant(
            @PathVariable UUID variantId
    ) {
        return ProductVariantResponse.from(
                productService.deactivateVariant(variantId)
        );
    }

    private boolean canViewUnpublished(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        Set.of("ROLE_ADMIN", "ROLE_SALES_STAFF")
                                .contains(authority.getAuthority())
                );
    }
}

