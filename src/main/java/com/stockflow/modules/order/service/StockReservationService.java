package com.stockflow.modules.order.service;

import com.stockflow.modules.inventory.entity.Inventory;
import com.stockflow.modules.inventory.repository.InventoryRepository;
import com.stockflow.modules.order.entity.Order;
import com.stockflow.modules.order.entity.OrderItem;
import com.stockflow.modules.order.entity.OrderStatus;
import com.stockflow.modules.order.entity.StockReservation;
import com.stockflow.modules.order.entity.StockReservationItem;
import com.stockflow.modules.order.entity.StockReservationItemStatus;
import com.stockflow.modules.order.entity.StockReservationStatus;
import com.stockflow.modules.order.repository.OrderItemRepository;
import com.stockflow.modules.order.repository.OrderRepository;
import com.stockflow.modules.order.repository.StockReservationItemRepository;
import com.stockflow.modules.order.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final StockReservationItemRepository stockReservationItemRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public StockReservationService(
            StockReservationRepository stockReservationRepository,
            StockReservationItemRepository stockReservationItemRepository,
            InventoryRepository inventoryRepository,
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository
    ) {
        this.stockReservationRepository = stockReservationRepository;
        this.stockReservationItemRepository = stockReservationItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Giữ hàng cho đơn hàng (reserve stock).
     *
     * @param order đơn hàng cần giữ tồn kho
     * @return bản ghi giữ hàng đã tạo
     */
    public StockReservation reserveStock(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException(
                    "Đơn hàng không hợp lệ hoặc chưa được lưu"
            );
        }

        // Khóa bản ghi Order để tránh hai request giữ hàng cùng lúc
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng"
                ));

        if (lockedOrder.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Chỉ có thể giữ hàng cho đơn hàng đang chờ thanh toán"
            );
        }

        // Kiểm tra reservation sau khi đã khóa Order
        if (stockReservationRepository.existsByOrder_Id(lockedOrder.getId())) {
            throw new IllegalStateException(
                    "Đơn hàng đã được giữ hàng trước đó"
            );
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(lockedOrder.getId());
        if (orderItems.isEmpty()) {
            throw new IllegalStateException("Đơn hàng không có sản phẩm nào để giữ hàng");
        }

        List<UUID> variantIds = orderItems.stream()
                .map(item -> item.getVariant().getId())
                .distinct()
                .toList();

        List<Inventory> inventories =
                inventoryRepository.findByWarehouseAndVariantsForUpdate(
                        lockedOrder.getFulfillmentWarehouse().getId(),
                        variantIds
                );

        if (inventories.size() != variantIds.size()) {
            throw new IllegalStateException(
                    "Một hoặc nhiều sản phẩm chưa được ghi nhận tồn kho trong kho được chọn"
            );
        }

        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(
                        inventory -> inventory.getVariant().getId(),
                        inventory -> inventory
                ));

        for (OrderItem item : orderItems) {
            Inventory inventory = inventoryMap.get(
                    item.getVariant().getId()
            );

            if (inventory.getAvailable() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for SKU: " + item.getSkuSnapshot()
                );
            }
        }

        for (OrderItem item : orderItems) {
            Inventory inventory = inventoryMap.get(
                    item.getVariant().getId()
            );

            inventory.reserveStock(item.getQuantity());
        }

        StockReservation reservation = new StockReservation(
                lockedOrder,
                lockedOrder.getFulfillmentWarehouse(),
                null
        );
        reservation = stockReservationRepository.save(reservation);

        List<StockReservationItem> reservationItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            Inventory inventory = inventoryMap.get(
                    item.getVariant().getId()
            );

            reservationItems.add(new StockReservationItem(
                    reservation,
                    item,
                    inventory,
                    item.getQuantity()
            ));
        }

        stockReservationItemRepository.saveAll(reservationItems);

        return reservation;
    }

    /**
     * Giải phóng tồn kho đã giữ (release reservation).
     * Sẽ triển khai chi tiết ở bước tiếp theo.
     *
     * @param orderId ID của đơn hàng cần giải phóng giữ hàng
     */
    public void releaseReservation(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException(
                    "Order ID không được để trống"
            );
        }

        // 1. Khóa Order
        Order lockedOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng"
                ));

        // 2. Khóa StockReservation
        StockReservation reservation =
                stockReservationRepository.findByOrderIdForUpdate(orderId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Đơn hàng chưa có thông tin giữ hàng"
                        ));

        // 3. Kiểm tra trạng thái
        if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Chỉ có thể giải phóng reservation đang ACTIVE"
            );
        }

        // 4. Khóa toàn bộ Reservation Items
        List<StockReservationItem> reservationItems =
                stockReservationItemRepository.findByReservationIdForUpdate(
                        reservation.getId()
                );

        if (reservationItems.isEmpty()) {
            throw new IllegalStateException(
                    "Reservation không có sản phẩm nào"
            );
        }

        // 5. Giải phóng số lượng đã giữ
        for (StockReservationItem item : reservationItems) {
            if (item.getStatus() != StockReservationItemStatus.ACTIVE) {
                continue;
            }

            Inventory inventory = inventoryRepository.findByIdForUpdate(
                    item.getInventory().getId()
            ).orElseThrow(() -> new IllegalStateException(
                    "Không tìm thấy Inventory tương ứng"
            ));

            inventory.releaseReservedStock(item.getQuantity());

            item.release();
        }

        reservation.release();
    }

    /**
     * Xác nhận xuất hàng đã giữ (consume reservation).
     * Sẽ triển khai chi tiết ở bước tiếp theo.
     *
     * @param orderId ID của đơn hàng cần xác nhận xuất hàng
     */
    public void consumeReservation(UUID orderId) {
        throw new UnsupportedOperationException("Chưa triển khai nghiệp vụ xác nhận xuất hàng");
    }
}
