package com.orderflow.order.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FrameworkExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(
                                fieldError ->
                                        fieldError.getField()
                                                + ": "
                                                + fieldError.getDefaultMessage()
                        )
                        .distinct()
                        .collect(
                                Collectors.joining("; ")
                        );

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message =
                exception
                        .getConstraintViolations()
                        .stream()
                        .map(
                                violation ->
                                        violation.getPropertyPath()
                                                + ": "
                                                + violation.getMessage()
                        )
                        .distinct()
                        .collect(
                                Collectors.joining("; ")
                        );

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is missing or malformed",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Invalid value for parameter '" + exception.getName() + "'",
                request
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_HEADER",
                "Required header '" + exception.getHeaderName() + "' is missing",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Required parameter '" + exception.getParameterName() + "' is missing",
                request
        );
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> handleInventoryResponseError(
            RestClientResponseException exception,
            HttpServletRequest request
    ) {
        int downstreamStatus =
                exception
                        .getStatusCode()
                        .value();

        String responseBody =
                exception
                        .getResponseBodyAsString();

        if (downstreamStatus == HttpStatus.CONFLICT.value()) {

            if (
                    responseBody != null
                            && responseBody.contains(
                            "INSUFFICIENT_STOCK"
                    )
            ) {
                return buildResponse(
                        HttpStatus.CONFLICT,
                        "INSUFFICIENT_STOCK",
                        "Requested inventory is not available",
                        request
                );
            }

            return buildResponse(
                    HttpStatus.CONFLICT,
                    "INVENTORY_CONFLICT",
                    "Inventory reservation conflicts with the current inventory state",
                    request
            );
        }

        if (downstreamStatus == HttpStatus.BAD_REQUEST.value()) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INVENTORY_REQUEST",
                    "Inventory rejected the reservation request",
                    request
            );
        }

        if (downstreamStatus == HttpStatus.NOT_FOUND.value()) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ORDER_ITEM",
                    "One or more requested products could not be found",
                    request
            );
        }

        if (
                downstreamStatus >= 500
                        || downstreamStatus == HttpStatus.UNAUTHORIZED.value()
                        || downstreamStatus == HttpStatus.FORBIDDEN.value()
        ) {
            return buildResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "INVENTORY_UNAVAILABLE",
                    "Inventory service is currently unavailable",
                    request
            );
        }

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "INVENTORY_BAD_RESPONSE",
                "Inventory service returned an unexpected response",
                request
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleInventoryConnectionError(
            ResourceAccessException exception,
            HttpServletRequest request
    ) {
        if (isTimeout(exception)) {
            return buildResponse(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "INVENTORY_TIMEOUT",
                    "Inventory service did not respond within the configured timeout",
                    request
            );
        }

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "INVENTORY_UNAVAILABLE",
                "Inventory service is currently unavailable",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "The request conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint",
                request
        );
    }

    private boolean isTimeout(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        while (current != null) {

            if (
                    current instanceof SocketTimeoutException
                            || current instanceof HttpTimeoutException
            ) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request
    ) {
        ApiError error =
                new ApiError(
                        Instant.now(),
                        status.value(),
                        errorCode,
                        message,
                        request.getRequestURI(),
                        resolveRequestId(request)
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    private String resolveRequestId(
            HttpServletRequest request
    ) {
        String requestId =
                request.getHeader(
                        "X-Request-Id"
                );

        if (
                requestId == null
                        || requestId.isBlank()
        ) {
            return null;
        }

        return requestId;
    }
}