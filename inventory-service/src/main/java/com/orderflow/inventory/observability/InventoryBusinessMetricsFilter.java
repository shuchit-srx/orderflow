package com.orderflow.inventory.observability;

import io.micrometer.core.instrument.MeterRegistry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InventoryBusinessMetricsFilter
        extends OncePerRequestFilter {

    private static final String RESERVATION_PATH =
            "/api/v1/internal/inventory/reservations";

    private final MeterRegistry meterRegistry;

    public InventoryBusinessMetricsFilter(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry =
                meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            if (
                    isReservationRequest(
                            request
                    )
                            && response.getStatus() >= 400
            ) {
                meterRegistry
                        .counter(
                                "orderflow.inventory.reservation.failures",
                                "status",
                                Integer.toString(
                                        response.getStatus()
                                )
                        )
                        .increment();
            }
        }
    }

    private boolean isReservationRequest(
            HttpServletRequest request
    ) {
        return "POST".equalsIgnoreCase(
                request.getMethod()
        )
                && RESERVATION_PATH.equals(
                request.getRequestURI()
        );
    }
}