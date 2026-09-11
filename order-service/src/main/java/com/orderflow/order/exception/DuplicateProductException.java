package com.orderflow.order.exception;

import java.util.UUID;

public class DuplicateProductException
        extends RuntimeException {

    public DuplicateProductException(
            UUID productId
    ) {

        super(
                "Product appears more than once in order: "
                        + productId
        );
    }
}