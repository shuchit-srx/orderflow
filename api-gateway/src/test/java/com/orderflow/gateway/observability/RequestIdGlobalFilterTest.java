package com.orderflow.gateway.observability;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdGlobalFilterTest {

    private static final String REQUEST_ID_HEADER =
            "X-Request-Id";

    private final Filter filter =
            new RequestIdGlobalFilter();

    @Test
    void shouldPreserveValidIncomingRequestId()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "GET"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        request.addHeader(
                REQUEST_ID_HEADER,
                "request-123"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> downstreamRequestId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        downstreamRequestId.set(
                                ((jakarta.servlet.http.HttpServletRequest)
                                        filteredRequest)
                                        .getHeader(
                                                REQUEST_ID_HEADER
                                        )
                        )
        );

        assertThat(
                downstreamRequestId.get()
        ).isEqualTo(
                "request-123"
        );

        assertThat(
                response.getHeader(
                        REQUEST_ID_HEADER
                )
        ).isEqualTo(
                "request-123"
        );
    }

    @Test
    void shouldGenerateRequestIdWhenMissing()
            throws Exception {

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

        AtomicReference<String> downstreamRequestId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        downstreamRequestId.set(
                                ((jakarta.servlet.http.HttpServletRequest)
                                        filteredRequest)
                                        .getHeader(
                                                REQUEST_ID_HEADER
                                        )
                        )
        );

        String requestId =
                downstreamRequestId.get();

        assertThat(
                requestId
        ).isNotBlank();

        UUID parsed =
                UUID.fromString(
                        requestId
                );

        assertThat(
                parsed.toString()
        ).isEqualTo(
                requestId
        );

        assertThat(
                response.getHeader(
                        REQUEST_ID_HEADER
                )
        ).isEqualTo(
                requestId
        );
    }

    @Test
    void shouldReplaceInvalidIncomingRequestId()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "POST"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        request.addHeader(
                REQUEST_ID_HEADER,
                "invalid request id"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> downstreamRequestId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        downstreamRequestId.set(
                                ((jakarta.servlet.http.HttpServletRequest)
                                        filteredRequest)
                                        .getHeader(
                                                REQUEST_ID_HEADER
                                        )
                        )
        );

        String requestId =
                downstreamRequestId.get();

        assertThat(
                requestId
        ).isNotBlank();

        assertThat(
                requestId
        ).isNotEqualTo(
                "invalid request id"
        );

        UUID.fromString(
                requestId
        );

        assertThat(
                response.getHeader(
                        REQUEST_ID_HEADER
                )
        ).isEqualTo(
                requestId
        );
    }

    @Test
    void shouldAllowSafeCorrelationIdCharacters()
            throws Exception {

        String suppliedRequestId =
                "order:abc-123_test.456";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(
                "GET"
        );

        request.setRequestURI(
                "/api/v1/orders"
        );

        request.addHeader(
                REQUEST_ID_HEADER,
                suppliedRequestId
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> downstreamRequestId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (filteredRequest, filteredResponse) ->
                        downstreamRequestId.set(
                                ((jakarta.servlet.http.HttpServletRequest)
                                        filteredRequest)
                                        .getHeader(
                                                REQUEST_ID_HEADER
                                        )
                        )
        );

        assertThat(
                downstreamRequestId.get()
        ).isEqualTo(
                suppliedRequestId
        );

        assertThat(
                response.getHeader(
                        REQUEST_ID_HEADER
                )
        ).isEqualTo(
                suppliedRequestId
        );
    }
}