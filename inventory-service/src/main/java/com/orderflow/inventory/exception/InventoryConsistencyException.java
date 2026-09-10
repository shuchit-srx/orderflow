package com.orderflow.inventory.exception;

import java.util.UUID;

public class InventoryConsistencyException
        extends RuntimeException {

    public InventoryConsistencyException(
            UUID productId
    ) {

        super(
                "Inventory state is inconsistent for product "
                        + productId
        );
    }
}