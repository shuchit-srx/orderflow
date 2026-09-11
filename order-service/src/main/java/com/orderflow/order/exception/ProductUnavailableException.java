package com.orderflow.order.exception;

import java.util.UUID;

public class ProductUnavailableException
        extends RuntimeException {

    public ProductUnavailableException(
            UUID productId
    ) {

        super(
                "Product is not available: "
                        + productId
        );
    }
}