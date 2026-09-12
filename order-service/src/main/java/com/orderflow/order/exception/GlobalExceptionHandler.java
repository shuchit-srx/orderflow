package com.orderflow.order.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Validation failed");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InvalidCustomerIdentityException.class
    )
    public ResponseEntity<ApiError>
    handleInvalidCustomerIdentity(

            InvalidCustomerIdentityException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CUSTOMER_IDENTITY",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InvalidOrderStateException.class
    )
    public ResponseEntity<ApiError>
    handleInvalidOrderState(

            InvalidOrderStateException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_ORDER_STATE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            ProductUnavailableException.class
    )
    public ResponseEntity<ApiError>
    handleProductUnavailable(

            ProductUnavailableException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_AVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InventoryServiceUnavailableException.class
    )
    public ResponseEntity<ApiError>
    handleInventoryUnavailable(

            InventoryServiceUnavailableException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "INVENTORY_SERVICE_UNAVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InventoryServiceException.class
    )
    public ResponseEntity<ApiError>
    handleInventoryServiceError(

            InventoryServiceException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "INVENTORY_SERVICE_ERROR",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            DuplicateProductException.class
    )
    public ResponseEntity<ApiError>
    handleDuplicateProduct(

            DuplicateProductException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "DUPLICATE_PRODUCT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InventoryReservationRejectedException.class
    )
    public ResponseEntity<ApiError>
    handleInventoryReservationRejected(

            InventoryReservationRejectedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVENTORY_RESERVATION_REJECTED",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            SagaCompensationException.class
    )
    public ResponseEntity<ApiError>
    handleSagaCompensationFailure(

                    SagaCompensationException exception,
                    HttpServletRequest request
            ) {

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SAGA_COMPENSATION_INCOMPLETE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InvalidIdempotencyKeyException.class
    )
    public ResponseEntity<ApiError>
    handleInvalidIdempotencyKey(
            InvalidIdempotencyKeyException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_IDEMPOTENCY_KEY",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IdempotencyKeyConflictException.class
    )
    public ResponseEntity<ApiError>
    handleIdempotencyConflict(
            IdempotencyKeyConflictException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_CONFLICT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IdempotencyRequestInProgressException.class
    )
    public ResponseEntity<ApiError>
    handleIdempotencyInProgress(
            IdempotencyRequestInProgressException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_REQUEST_IN_PROGRESS",
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

        return ResponseEntity
                .status(status)
                .body(
                        new ApiError(
                                Instant.now(),
                                status.value(),
                                error,
                                message,
                                path
                        )
                );
    }
}