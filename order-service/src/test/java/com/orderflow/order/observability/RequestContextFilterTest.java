package com.orderflow.order.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.slf4j.MDC;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextFilterTest {

    private final RequestContextFilter filter =
            new RequestContextFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPreserveIncomingRequestId() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        request.addHeader(
                RequestContextFilter.REQUEST_ID_HEADER,
                "order-request-123"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) -> {
                    assertThat(
                            ((HttpServletRequest) filteredRequest).getHeader(
                                    RequestContextFilter.REQUEST_ID_HEADER
                            )
                    ).isEqualTo(
                            "order-request-123"
                    );

                    assertThat(
                            MDC.get(
                                    RequestContextFilter
                                            .REQUEST_ID_MDC_KEY
                            )
                    ).isEqualTo(
                            "order-request-123"
                    );

                    ((HttpServletResponse) filteredResponse)
                            .setStatus(
                                    201
                            );
                }
        );

        assertThat(
                response.getHeader(
                        RequestContextFilter
                                .REQUEST_ID_HEADER
                )
        ).isEqualTo(
                "order-request-123"
        );

        assertThat(
                MDC.get(
                        RequestContextFilter
                                .REQUEST_ID_MDC_KEY
                )
        ).isNull();
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() throws Exception {

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

        AtomicReference<String> observedRequestId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        observedRequestId.set(
                                ((HttpServletRequest) filteredRequest).getHeader(
                                        RequestContextFilter.REQUEST_ID_HEADER
                                )
                        )
        );

        String generatedRequestId =
                observedRequestId.get();

        assertThat(
                generatedRequestId
        ).isNotBlank();

        UUID.fromString(
                generatedRequestId
        );

        assertThat(
                response.getHeader(
                        RequestContextFilter
                                .REQUEST_ID_HEADER
                )
        ).isEqualTo(
                generatedRequestId
        );
    }

    @Test
    void shouldClearMdcAfterRequestCompletes() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "GET"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        request.addHeader(
                RequestContextFilter.REQUEST_ID_HEADER,
                "cleanup-test"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        assertThat(
                                MDC.get(
                                        RequestContextFilter
                                                .REQUEST_ID_MDC_KEY
                                )
                        ).isEqualTo(
                                "cleanup-test"
                        )
        );

        assertThat(
                MDC.get(
                        RequestContextFilter
                                .REQUEST_ID_MDC_KEY
                )
        ).isNull();
    }
}