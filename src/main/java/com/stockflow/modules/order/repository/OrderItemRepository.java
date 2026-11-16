package com.stockflow.modules.order.repository;

import com.stockflow.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    // Get all items belonging to an order
    List<OrderItem> findByOrder_Id(UUID orderId);

    // Count the number of items in an order
    long countByOrder_Id(UUID orderId);
}
