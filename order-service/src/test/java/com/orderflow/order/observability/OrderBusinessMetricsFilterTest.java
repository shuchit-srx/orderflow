package com.orderflow.order.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBusinessMetricsFilterTest {

    private final SimpleMeterRegistry meterRegistry =
            new SimpleMeterRegistry();

    private final OrderBusinessMetricsFilter filter =
            new OrderBusinessMetricsFilter(
                    meterRegistry
            );

    @AfterEach
    void closeRegistry() {
        meterRegistry.close();
    }

    @Test
    void shouldIncrementOrderFailureMetric() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        ((HttpServletResponse) filteredResponse)
                                .setStatus(
                                        409
                                )
        );

        Counter counter =
                meterRegistry
                        .find(
                                "orderflow.orders.failures"
                        )
                        .tag(
                                "status",
                                "409"
                        )
                        .counter();

        assertThat(
                counter
        ).isNotNull();

        assertThat(
                counter.count()
        ).isEqualTo(
                1.0
        );
    }

    @Test
    void shouldNotIncrementMetricForSuccessfulOrder() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        ((HttpServletResponse) filteredResponse)
                                .setStatus(
                                        201
                                )
        );

        Counter counter =
                meterRegistry
                        .find(
                                "orderflow.orders.failures"
                        )
                        .counter();

        assertThat(
                counter
        ).isNull();
    }

    @Test
    void shouldNotCountFailureForOtherOrderEndpoints() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "GET"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        ((HttpServletResponse) filteredResponse)
                                .setStatus(
                                        500
                                )
        );

        Counter counter =
                meterRegistry
                        .find(
                                "orderflow.orders.failures"
                        )
                        .counter();

        assertThat(
                counter
        ).isNull();
    }
}