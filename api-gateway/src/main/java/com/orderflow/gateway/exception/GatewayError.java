package com.orderflow.gateway.exception;

import java.time.Instant;

public record GatewayError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}