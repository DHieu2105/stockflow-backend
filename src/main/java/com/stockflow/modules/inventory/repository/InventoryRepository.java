package com.stockflow.modules.inventory.repository;

import com.stockflow.modules.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    @EntityGraph(attributePaths = {"warehouse", "variant"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i FROM Inventory i
        WHERE i.warehouse.id = :warehouseId
          AND i.variant.id = :variantId
        """)
    Optional<Inventory> findByWarehouseAndVariantForUpdate(
        @Param("warehouseId") UUID warehouseId,
        @Param("variantId") UUID variantId
    );

    @EntityGraph(attributePaths = {"warehouse", "variant"})
    Optional<Inventory> findByWarehouse_IdAndVariant_Id(
        UUID warehouseId,
        UUID variantId
    );

    @EntityGraph(attributePaths = {"warehouse", "variant"})
    List<Inventory> findByWarehouse_Id(UUID warehouseId);

    @EntityGraph(attributePaths = {"warehouse", "variant"})
    Page<Inventory> findByWarehouse_Id(
        UUID warehouseId,
        Pageable pageable
    );
}

