package com.stockflow.modules.order.entity;

import com.stockflow.modules.product.entity.ProductVariant;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "sku_snapshot", nullable = false, length = 100)
    private String skuSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "variant_name_snapshot", nullable = false, length = 255)
    private String variantNameSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(
        name = "line_total",
        insertable = false,
        updatable = false
    )
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(
            Order order,
            ProductVariant variant,
            String skuSnapshot,
            String productNameSnapshot,
            String variantNameSnapshot,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Order item quantity must be greater than zero"
            );
        }

        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price must not be negative"
            );
        }

        this.order = order;
        this.variant = variant;
        this.skuSnapshot = skuSnapshot;
        this.productNameSnapshot = productNameSnapshot;
        this.variantNameSnapshot = variantNameSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public String getVariantNameSnapshot() {
        return variantNameSnapshot;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
