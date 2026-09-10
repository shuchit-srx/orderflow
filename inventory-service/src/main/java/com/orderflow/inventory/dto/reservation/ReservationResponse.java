package com.orderflow.inventory.dto.reservation;

import com.orderflow.inventory.domain.InventoryReservation;
import com.orderflow.inventory.domain.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID orderId,
        ReservationStatus status,
        Instant expiresAt,
        List<ReservationItemResponse> items
) {

    public static ReservationResponse from(
            InventoryReservation reservation
    ) {

        List<ReservationItemResponse> items =
                reservation
                        .getItems()
                        .stream()
                        .map(ReservationItemResponse::from)
                        .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                items
        );
    }
}