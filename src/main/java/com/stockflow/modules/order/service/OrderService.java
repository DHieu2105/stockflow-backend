package com.stockflow.modules.order.service;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.order.dto.CreateOrderRequest;
import com.stockflow.modules.order.dto.OrderItemRequest;
import com.stockflow.modules.order.dto.OrderItemResponse;
import com.stockflow.modules.order.dto.OrderResponse;
import com.stockflow.modules.order.entity.Order;
import com.stockflow.modules.order.entity.OrderItem;
import com.stockflow.modules.order.entity.OrderStatus;
import com.stockflow.modules.order.repository.OrderRepository;
import com.stockflow.modules.product.entity.ProductStatus;
import com.stockflow.modules.product.entity.ProductVariant;
import com.stockflow.modules.product.repository.ProductVariantRepository;
import com.stockflow.modules.warehouse.entity.Warehouse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final StockReservationService stockReservationService;

    public OrderService(
            ProductVariantRepository productVariantRepository,
            WarehouseSelectionService warehouseSelectionService,
            OrderRepository orderRepository,
            StockReservationService stockReservationService
    ) {
        this.productVariantRepository = productVariantRepository;
        this.warehouseSelectionService = warehouseSelectionService;
        this.orderRepository = orderRepository;
        this.stockReservationService = stockReservationService;
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

        for (Map.Entry<UUID, Integer> entry : items.entrySet()) {
            int totalQuantity = entry.getValue();

            if (totalQuantity <= 0) {
                throw new IllegalArgumentException(
                        "Tổng số lượng của mỗi biến thể sản phẩm phải lớn hơn 0"
                );
            }

            if (totalQuantity > 999) {
                throw new IllegalArgumentException(
                        "Tổng số lượng cho một biến thể sản phẩm không được vượt quá 999"
                );
            }
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
            Warehouse warehouse,
            Map<UUID, Integer> requestedItems,
            Map<UUID, ProductVariant> variantMap
    ) {
        Order order = createOrder(request, customer, warehouse);

        List<OrderItem> orderItems = buildOrderItems(
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

    @Transactional(readOnly = true)
    public OrderResponse getOrderForCustomer(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng"
                ));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForStaff(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng"
                ));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(
            UUID customerId,
            OrderStatus status,
            Pageable pageable
    ) {
        Page<Order> orders = status == null
                ? orderRepository.findByCustomer_IdOrderByCreatedAtDesc(
                        customerId,
                        pageable
                )
                : orderRepository.findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                        customerId,
                        status,
                        pageable
                );

        return orders.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            OrderStatus status,
            Pageable pageable
    ) {
        Page<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(
                        status,
                        pageable
                );

        return orders.map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            User customer
    ) {
        // 1. Chuẩn hóa danh sách sản phẩm
        Map<UUID, Integer> requestedItems = normalizeItems(request);

        // 2. Kiểm tra và load ProductVariant
        Map<UUID, ProductVariant> variantMap =
                validateAndLoadVariants(requestedItems);

        // 3. Chọn kho đủ hàng
        Warehouse warehouse =
                warehouseSelectionService.selectWarehouse(requestedItems);

        // 4. Chuẩn bị Order và OrderItems
        Order order = prepareOrder(
                request,
                customer,
                warehouse,
                requestedItems,
                variantMap
        );

        // 5. Lưu Order và OrderItems xuống database
        Order savedOrder = orderRepository.saveAndFlush(order);

        // 6. Giữ tồn kho
        stockReservationService.reserveStock(savedOrder);

        // 7. Chuyển entity thành response
        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getLineTotal() != null
                            ? item.getLineTotal()
                            : item.getUnitPrice().multiply(
                                    BigDecimal.valueOf(item.getQuantity())
                            );

                    return new OrderItemResponse(
                            item.getId(),
                            item.getVariant().getId(),
                            item.getSkuSnapshot(),
                            item.getProductNameSnapshot(),
                            item.getVariantNameSnapshot(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            lineTotal
                    );
                })
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingAddress(),
                order.getNote(),
                items,
                order.getCreatedAt()
        );
    }
}
