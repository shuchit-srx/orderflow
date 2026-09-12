package com.orderflow.order.observability;

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
public class OrderBusinessMetricsFilter
        extends OncePerRequestFilter {

    private static final String CREATE_ORDER_PATH =
            "/api/v1/orders";

    private final MeterRegistry meterRegistry;

    public OrderBusinessMetricsFilter(
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
                    isCreateOrderRequest(
                            request
                    )
                            && response.getStatus() >= 400
            ) {
                meterRegistry
                        .counter(
                                "orderflow.orders.failures",
                                "status",
                                Integer.toString(
                                        response.getStatus()
                                )
                        )
                        .increment();
            }
        }
    }

    private boolean isCreateOrderRequest(
            HttpServletRequest request
    ) {
        return "POST".equalsIgnoreCase(
                request.getMethod()
        )
                && CREATE_ORDER_PATH.equals(
                request.getRequestURI()
        );
    }
}