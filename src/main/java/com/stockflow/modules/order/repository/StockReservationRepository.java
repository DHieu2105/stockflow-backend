package com.stockflow.modules.order.repository;

import com.stockflow.modules.order.entity.StockReservation;
import com.stockflow.modules.order.entity.StockReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository
        extends JpaRepository<StockReservation, UUID> {

    // Find reservation belonging to an order
    Optional<StockReservation> findByOrder_Id(UUID orderId);

    // Check whether an order already has a reservation
    boolean existsByOrder_Id(UUID orderId);

    // Find active reservations that have expired
    List<StockReservation> findByStatusAndExpiresAtBefore(
            StockReservationStatus status,
            Instant currentTime
    );

    // Lock reservation before changing its status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM StockReservation sr
            WHERE sr.id = :reservationId
            """)
    Optional<StockReservation> findByIdForUpdate(
            @Param("reservationId") UUID reservationId
    );

    // Lock reservation by order ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM StockReservation sr
            WHERE sr.order.id = :orderId
            """)
    Optional<StockReservation> findByOrderIdForUpdate(
            @Param("orderId") UUID orderId
    );
}
