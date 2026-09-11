package com.orderflow.order.exception;

import com.orderflow.order.domain.OrderStatus;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(
            UUID orderId,
            OrderStatus currentStatus,
            OrderStatus targetStatus
    ) {
        super(
                "Order " + orderId
                        + " cannot transition from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}