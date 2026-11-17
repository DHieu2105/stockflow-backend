package com.stockflow.modules.order.service;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.order.dto.CreateOrderRequest;
import com.stockflow.modules.order.dto.OrderItemRequest;
import com.stockflow.modules.order.entity.Order;
import com.stockflow.modules.order.entity.OrderItem;
import com.stockflow.modules.order.repository.OrderRepository;
import com.stockflow.modules.product.entity.ProductStatus;
import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.repository.ProductVariantRepository;
import com.stockflow.modules.warehouse.entity.Warehouse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ProductVariantRepository productVariantRepository;
    private final WarehouseSelectionService warehouseSelectionService;
    private final OrderRepository orderRepository;

    public OrderService(
            ProductVariantRepository productVariantRepository,
            WarehouseSelectionService warehouseSelectionService,
            OrderRepository orderRepository
    ) {
        this.productVariantRepository = productVariantRepository;
        this.warehouseSelectionService = warehouseSelectionService;
        this.orderRepository = orderRepository;
    }

    private Map<UUID, Integer> normalizeItems(
            CreateOrderRequest request) {
        Map<UUID, Integer> items = new LinkedHashMap<>();

        for (OrderItemRequest item : request.items()) {
            items.merge(
                    item.variantId(),
                    item.quantity(),
                    Integer::sum);
        }

        return items;
    }

    private Map<UUID, ProductVariant> validateAndLoadVariants(
            Map<UUID, Integer> requestedItems) {
        List<ProductVariant> variants = productVariantRepository.findByIdIn(
                requestedItems.keySet());

        if (variants.size() != requestedItems.size()) {
            throw new IllegalArgumentException(
                    "Một hoặc nhiều sản phẩm không tồn tại");
        }

        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(
                        ProductVariant::getId,
                        variant -> variant));

        for (UUID variantId : requestedItems.keySet()) {
            ProductVariant variant = variantMap.get(variantId);

            if (variant == null) {
                throw new IllegalArgumentException(
                        "Sản phẩm không tồn tại: " + variantId);
            }

            if (variant.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Sản phẩm đã ngừng kinh doanh: "
                                + variant.getSku());
            }

            if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Sản phẩm không còn được kinh doanh: "
                                + variant.getProduct().getName());
            }
        }

        return variantMap;
    }

    private List<OrderItem> buildOrderItems(
            Order order,
            Map<UUID, Integer> requestedItems,
            Map<UUID, ProductVariant> variantMap
    ) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : requestedItems.entrySet()) {
            ProductVariant variant = variantMap.get(entry.getKey());

            OrderItem orderItem = new OrderItem(
                    null,
                    variant,
                    variant.getSku(),
                    variant.getProduct().getName(),
                    variant.getVariantName(),
                    entry.getValue(),
                    variant.getPrice()
            );

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private BigDecimal calculateSubtotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderCode() {
        return "SF-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private Order createOrder(
            CreateOrderRequest request,
            User customer,
            Warehouse warehouse
    ) {
        Map<String, Object> address = new HashMap<>();

        address.put("addressLine", request.shippingAddress().addressLine());
        address.put("ward", request.shippingAddress().ward());
        address.put("district", request.shippingAddress().district());
        address.put("province", request.shippingAddress().province());

        return new Order(
                generateOrderCode(),
                customer,
                warehouse,
                request.recipientName(),
                request.recipientPhone(),
                address,
                request.note()
        );
    }

    private Order prepareOrder(
            CreateOrderRequest request,
            User customer,
            Warehouse warehouse
    ) {
        Map<UUID, Integer> requestedItems = normalizeItems(request);

        Map<UUID, ProductVariant> variantMap =
                validateAndLoadVariants(requestedItems);

        Order order = createOrder(request, customer, warehouse);

        List<OrderItem> orderItems = buildOrderItems(
                order,
                requestedItems,
                variantMap
        );

        BigDecimal subtotal = calculateSubtotal(orderItems);

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        order.updateAmounts(
                subtotal,
                shippingFee,
                discountAmount
        );

        orderItems.forEach(order::addItem);

        return order;
    }
}
