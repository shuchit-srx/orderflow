package com.orderflow.order.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.slf4j.MDC;

import org.springframework.http.MediaType;

import org.springframework.test.web.client.MockRestServiceServer;

import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RequestIdRestClientCustomizerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPropagateRequestIdToRestClientRequest() {

        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://inventory-service"
                        );

        RequestIdRestClientCustomizer customizer =
                new RequestIdRestClientCustomizer();

        customizer.customize(
                builder
        );

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(
                                builder
                        )
                        .build();

        server.expect(
                        requestTo(
                                "http://inventory-service/api/v1/internal/inventory/reservations"
                        )
                )
                .andExpect(
                        header(
                                RequestContextFilter.REQUEST_ID_HEADER,
                                "distributed-request-123"
                        )
                )
                .andRespond(
                        withSuccess(
                                "{}",
                                MediaType.APPLICATION_JSON
                        )
                );

        MDC.put(
                RequestContextFilter.REQUEST_ID_MDC_KEY,
                "distributed-request-123"
        );

        RestClient restClient =
                builder.build();

        restClient
                .post()
                .uri(
                        "/api/v1/internal/inventory/reservations"
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .body(
                        "{}"
                )
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }
}