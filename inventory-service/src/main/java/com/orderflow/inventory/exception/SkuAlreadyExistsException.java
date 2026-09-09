package com.orderflow.inventory.exception;

public class SkuAlreadyExistsException
        extends RuntimeException {

    public SkuAlreadyExistsException(
            String sku
    ) {
        super(
                "Product SKU already exists: "
                        + sku
        );
    }
}