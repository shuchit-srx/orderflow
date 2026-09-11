package com.orderflow.order.exception;

import java.util.UUID;

public class SagaCompensationException
        extends RuntimeException {

    private final UUID orderId;

    public SagaCompensationException(
            UUID orderId,
            Throwable originalFailure,
            Throwable compensationFailure
    ) {

        super(
                "Saga compensation could not complete for order: "
                        + orderId,
                originalFailure
        );

        this.orderId = orderId;

        addSuppressed(
                compensationFailure
        );
    }

    public UUID getOrderId() {
        return orderId;
    }
}