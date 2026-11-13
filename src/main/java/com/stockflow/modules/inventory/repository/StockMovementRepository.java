package com.stockflow.modules.inventory.repository;

import com.stockflow.modules.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @EntityGraph(attributePaths = {
        "inventory",
        "inventory.warehouse",
        "inventory.variant",
        "createdBy"
    })
    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.inventory.warehouse.id = :warehouseId
        ORDER BY sm.createdAt DESC
        """)
    Page<StockMovement> findByWarehouse_Id(
        @Param("warehouseId") UUID warehouseId,
        Pageable pageable
    );

    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.inventory.warehouse.id = :warehouseId
          AND sm.inventory.variant.id = :variantId
        ORDER BY sm.createdAt DESC
        """)
    Page<StockMovement> findByWarehouse_IdAndVariant_Id(
        @Param("warehouseId") UUID warehouseId,
        @Param("variantId") UUID variantId,
        Pageable pageable
    );

    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.inventory.variant.id = :variantId
        ORDER BY sm.createdAt DESC
        """)
    List<StockMovement> findByVariant_Id(@Param("variantId") UUID variantId);

    Page<StockMovement> findByInventory_Id(UUID inventoryId, Pageable pageable);
}
