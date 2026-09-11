package com.orderflow.order.exception;

public class InvalidCustomerIdentityException
        extends RuntimeException {

    public InvalidCustomerIdentityException() {
        super(
                "Authenticated customer identity is invalid"
        );
    }
}