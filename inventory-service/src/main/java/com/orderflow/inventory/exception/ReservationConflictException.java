package com.orderflow.inventory.exception;

import java.util.UUID;

public class ReservationConflictException
        extends RuntimeException {

    public ReservationConflictException(
            UUID orderId
    ) {

        super(
                "A different reservation request already exists for order "
                        + orderId
        );
    }
}