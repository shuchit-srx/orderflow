package com.orderflow.inventory.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryBusinessMetricsFilterTest {

    private final SimpleMeterRegistry meterRegistry =
            new SimpleMeterRegistry();

    private final InventoryBusinessMetricsFilter filter =
            new InventoryBusinessMetricsFilter(
                    meterRegistry
            );

    @AfterEach
    void closeRegistry() {
        meterRegistry.close();
    }

    @Test
    void shouldIncrementReservationFailureMetric() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/internal/inventory/reservations"
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
                                "orderflow.inventory.reservation.failures"
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
    void shouldNotIncrementMetricForSuccessfulReservation() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/internal/inventory/reservations"
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
                                "orderflow.inventory.reservation.failures"
                        )
                        .counter();

        assertThat(
                counter
        ).isNull();
    }

    @Test
    void shouldNotCountFailuresFromOtherEndpoints() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "GET"
        );

        request.setRequestURI(
                "/api/v1/products"
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
                                "orderflow.inventory.reservation.failures"
                        )
                        .counter();

        assertThat(
                counter
        ).isNull();
    }
}