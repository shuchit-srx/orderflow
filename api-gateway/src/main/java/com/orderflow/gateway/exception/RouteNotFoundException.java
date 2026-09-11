package com.orderflow.gateway.exception;

public class RouteNotFoundException
        extends RuntimeException {

    public RouteNotFoundException(
            String path
    ) {

        super(
                "No public gateway route for: "
                        + path
        );
    }
}