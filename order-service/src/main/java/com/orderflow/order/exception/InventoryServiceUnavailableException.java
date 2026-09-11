package com.orderflow.order.exception;

public class InventoryServiceUnavailableException
        extends RuntimeException {

    public InventoryServiceUnavailableException(
            Throwable cause
    ) {

        super(
                "Inventory Service is unavailable",
                cause
        );
    }
}