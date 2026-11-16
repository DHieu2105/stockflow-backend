package com.stockflow.modules.warehouse.repository;

import com.stockflow.modules.warehouse.entity.Warehouse;
import com.stockflow.modules.warehouse.entity.WarehouseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w FROM Warehouse w
        WHERE w.id = :warehouseId
        """)
    Optional<Warehouse> findByIdForUpdate(
            @Param("warehouseId") UUID warehouseId
    );

    boolean existsByCode(String code);

    List<Warehouse> findByStatus(WarehouseStatus status);

    @Query(value = """
            SELECT COUNT(*) FROM inventories
            WHERE warehouse_id = :warehouseId AND (on_hand > 0 OR reserved > 0)
            """, nativeQuery = true)
    long countActiveInventory(@Param("warehouseId") UUID warehouseId);

    @Query(value = """
            SELECT COUNT(*) FROM orders
            WHERE fulfillment_warehouse_id = :warehouseId
              AND status IN ('PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPED')
            """, nativeQuery = true)
    long countPendingOrders(@Param("warehouseId") UUID warehouseId);
}
