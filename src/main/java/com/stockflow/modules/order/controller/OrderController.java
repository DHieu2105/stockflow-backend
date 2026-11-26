package com.stockflow.modules.order.controller;

import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.identity.repository.UserRepository;
import com.stockflow.modules.order.dto.CreateOrderRequest;
import com.stockflow.modules.order.dto.OrderResponse;
import com.stockflow.modules.order.entity.OrderStatus;
import com.stockflow.modules.order.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(
            OrderService orderService,
            UserRepository userRepository
    ) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        UUID customerId = UUID.fromString(authentication.getName());

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy tài khoản khách hàng"
                ));

        OrderResponse response = orderService.createOrder(
                request,
                customer
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALES_STAFF')")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        OrderResponse response = hasStaffAccess(authentication)
                ? orderService.getOrderForStaff(orderId)
                : orderService.getOrderForCustomer(
                        orderId,
                        UUID.fromString(authentication.getName())
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALES_STAFF')")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable,
            Authentication authentication
    ) {
        Page<OrderResponse> response = hasStaffAccess(authentication)
                ? orderService.getAllOrders(status, pageable)
                : orderService.getCustomerOrders(
                        UUID.fromString(authentication.getName()),
                        status,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    private boolean hasStaffAccess(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equals("ROLE_ADMIN")
                                || authority.equals("ROLE_SALES_STAFF")
                );
    }
}
