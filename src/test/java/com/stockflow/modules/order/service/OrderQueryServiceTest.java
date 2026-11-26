package com.stockflow.modules.order.service;

import com.stockflow.modules.order.dto.OrderResponse;
import com.stockflow.modules.order.entity.Order;
import com.stockflow.modules.order.entity.OrderStatus;
import com.stockflow.modules.order.repository.OrderRepository;
import com.stockflow.modules.product.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private WarehouseSelectionService warehouseSelectionService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockReservationService stockReservationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void customerOrderLookupIsScopedToTheRequestingCustomer() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order order = orderResponseFixture(orderId);
        when(orderRepository.findByIdAndCustomer_Id(orderId, customerId))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.getOrderForCustomer(orderId, customerId);

        assertEquals(orderId, response.id());
        verify(orderRepository).findByIdAndCustomer_Id(orderId, customerId);
        verify(orderRepository, never()).findById(orderId);
    }

    @Test
    void customerCannotRetrieveAnotherCustomersOrder() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderRepository.findByIdAndCustomer_Id(orderId, customerId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> orderService.getOrderForCustomer(orderId, customerId)
        );

        verify(orderRepository).findByIdAndCustomer_Id(orderId, customerId);
        verify(orderRepository, never()).findById(orderId);
    }

    @Test
    void customerOrderHistoryUsesStatusAndPaginationFilters() {
        UUID customerId = UUID.randomUUID();
        OrderStatus status = OrderStatus.PENDING_PAYMENT;
        var pageable = PageRequest.of(1, 10);
        when(orderRepository.findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                customerId,
                status,
                pageable
        )).thenReturn(Page.empty(pageable));

        Page<OrderResponse> response =
                orderService.getCustomerOrders(customerId, status, pageable);

        assertEquals(1, response.getNumber());
        assertEquals(10, response.getSize());
        verify(orderRepository)
                .findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                        customerId,
                        status,
                        pageable
                );
        verify(orderRepository, never())
                .findByCustomer_IdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Test
    void staffOrderHistoryCanFilterAllOrdersByStatus() {
        OrderStatus status = OrderStatus.CONFIRMED;
        var pageable = PageRequest.of(0, 5);
        when(orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable))
                .thenReturn(Page.empty(pageable));

        Page<OrderResponse> response =
                orderService.getAllOrders(status, pageable);

        assertEquals(0, response.getTotalElements());
        verify(orderRepository)
                .findByStatusOrderByCreatedAtDesc(status, pageable);
        verify(orderRepository, never())
                .findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void staffCanRetrieveAnyOrderById() {
        UUID orderId = UUID.randomUUID();
        Order order = orderResponseFixture(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderForStaff(orderId);

        assertEquals(orderId, response.id());
        verify(orderRepository).findById(orderId);
    }

    private Order orderResponseFixture(UUID orderId) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getSubtotal()).thenReturn(BigDecimal.ZERO);
        when(order.getShippingFee()).thenReturn(BigDecimal.ZERO);
        when(order.getDiscountAmount()).thenReturn(BigDecimal.ZERO);
        when(order.getTotalAmount()).thenReturn(BigDecimal.ZERO);
        when(order.getShippingAddress()).thenReturn(Map.of());
        when(order.getItems()).thenReturn(List.of());
        when(order.getCreatedAt()).thenReturn(Instant.now());
        return order;
    }
}
