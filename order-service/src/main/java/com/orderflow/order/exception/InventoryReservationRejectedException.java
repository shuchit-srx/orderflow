package com.orderflow.order.exception;

import java.util.UUID;

public class InventoryReservationRejectedException
        extends RuntimeException {

    private final UUID orderId;

    public InventoryReservationRejectedException(
            UUID orderId
    ) {

        super(
                "Inventory reservation was rejected for order: "
                        + orderId
        );

        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}