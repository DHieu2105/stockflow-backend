package com.stockflow.modules.order.repository;

import com.stockflow.modules.order.entity.StockReservationItem;
import com.stockflow.modules.order.entity.StockReservationItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockReservationItemRepository
        extends JpaRepository<StockReservationItem, UUID> {

    // Get all reservation items belonging to a reservation
    List<StockReservationItem> findByReservation_Id(UUID reservationId);

    // Get active items belonging to a reservation
    List<StockReservationItem> findByReservation_IdAndStatus(
            UUID reservationId,
            StockReservationItemStatus status
    );

    // Lock all items before releasing or consuming reservation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sri FROM StockReservationItem sri
            WHERE sri.reservation.id = :reservationId
            ORDER BY sri.id
            """)
    List<StockReservationItem> findByReservationIdForUpdate(
            @Param("reservationId") UUID reservationId
    );
}
