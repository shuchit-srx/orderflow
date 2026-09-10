package com.orderflow.inventory.exception;

import java.util.UUID;

public class InsufficientStockException
        extends RuntimeException {

    public InsufficientStockException(
            UUID productId,
            int availableQuantity,
            int requestedReduction
    ) {

        super(
                "Cannot reduce inventory for product "
                        + productId
                        + " by "
                        + requestedReduction
                        + ". Available quantity is "
                        + availableQuantity
        );
    }
}