package com.orderflow.gateway.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(
            RouteNotFoundException.class
    )
    public ResponseEntity<GatewayError>
    handleRouteNotFound(

            RouteNotFoundException exception,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            UpstreamUnavailableException.class
    )
    public ResponseEntity<GatewayError>
    handleUnavailable(

            UpstreamUnavailableException exception,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "UPSTREAM_UNAVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<GatewayError> build(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {

        return ResponseEntity
                .status(status)
                .body(
                        new GatewayError(
                                Instant.now(),
                                status.value(),
                                error,
                                message,
                                path
                        )
                );
    }
}