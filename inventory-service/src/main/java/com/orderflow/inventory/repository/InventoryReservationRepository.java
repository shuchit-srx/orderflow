package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.InventoryReservation;
import com.orderflow.inventory.domain.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByOrderId(
            UUID orderId
    );

    boolean existsByOrderId(
            UUID orderId
    );

    List<InventoryReservation>
    findByStatusAndExpiresAtBefore(
            ReservationStatus status,
            Instant time
    );
}