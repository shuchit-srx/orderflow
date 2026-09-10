package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.InventoryReservation;
import com.orderflow.inventory.domain.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByOrderId(
            UUID orderId
    );

    boolean existsByOrderId(
            UUID orderId
    );

    List<InventoryReservation>
    findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            ReservationStatus status,
            Instant time
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("""
        SELECT r
        FROM InventoryReservation r
        WHERE r.orderId = :orderId
        """)
    Optional<InventoryReservation> findByOrderIdForUpdate(
            @Param("orderId")
            UUID orderId
    );
}