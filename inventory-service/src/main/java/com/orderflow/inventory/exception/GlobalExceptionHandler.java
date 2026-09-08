package com.orderflow.inventory.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(
            ProductNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ApiError> handleInventoryNotFound(
            InventoryNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "INVENTORY_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {

        ApiError response =
                new ApiError(
                        Instant.now(),
                        status.value(),
                        error,
                        message,
                        path
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}