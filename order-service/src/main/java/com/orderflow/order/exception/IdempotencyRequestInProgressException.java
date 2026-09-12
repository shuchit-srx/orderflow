package com.orderflow.order.exception;

public class IdempotencyRequestInProgressException
        extends RuntimeException {

    public IdempotencyRequestInProgressException(
            String idempotencyKey
    ) {

        super(
                "A request with this idempotency key is already being processed: "
                        + idempotencyKey
        );
    }
}