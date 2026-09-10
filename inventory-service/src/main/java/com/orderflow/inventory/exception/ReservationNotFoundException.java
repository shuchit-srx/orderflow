package com.orderflow.inventory.exception;

import java.util.UUID;

public class ReservationNotFoundException
        extends RuntimeException {

    public ReservationNotFoundException(
            UUID orderId
    ) {

        super(
                "Inventory reservation not found for order "
                        + orderId
        );
    }
}