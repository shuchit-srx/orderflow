package com.orderflow.inventory.exception;

import com.orderflow.inventory.domain.ReservationStatus;

import java.util.UUID;

public class InvalidReservationStateException
        extends RuntimeException {

    public InvalidReservationStateException(
            UUID orderId,
            ReservationStatus status,
            String operation
    ) {

        super(
                "Cannot "
                        + operation
                        + " reservation for order "
                        + orderId
                        + " while reservation status is "
                        + status
        );
    }
}