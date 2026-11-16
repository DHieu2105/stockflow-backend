package com.stockflow.modules.order.repository;

import com.stockflow.modules.order.entity.Order;
import com.stockflow.modules.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Find order by its business code
    Optional<Order> findByOrderCode(String orderCode);

    // Check whether an order code already exists
    boolean existsByOrderCode(String orderCode);

    // Get all orders belonging to a customer
    Page<Order> findByCustomer_IdOrderByCreatedAtDesc(
            UUID customerId,
            Pageable pageable
    );

    // Get customer orders filtered by status
    Page<Order> findByCustomer_IdAndStatusOrderByCreatedAtDesc(
            UUID customerId,
            OrderStatus status,
            Pageable pageable
    );

    // Find an order with a pessimistic write lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM Order o
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId") UUID orderId
    );
}
