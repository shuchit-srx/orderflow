package com.orderflow.order.observability;

import org.slf4j.MDC;

import org.springframework.boot.restclient.RestClientCustomizer;

import org.springframework.stereotype.Component;

import org.springframework.web.client.RestClient;

@Component
public class RequestIdRestClientCustomizer
        implements RestClientCustomizer {

    @Override
    public void customize(
            RestClient.Builder restClientBuilder
    ) {
        restClientBuilder
                .requestInterceptor(
                        (
                                request,
                                body,
                                execution
                        ) -> {
                            String requestId =
                                    MDC.get(
                                            RequestContextFilter
                                                    .REQUEST_ID_MDC_KEY
                                    );

                            if (
                                    requestId != null
                                            && !requestId.isBlank()
                            ) {
                                request
                                        .getHeaders()
                                        .set(
                                                RequestContextFilter
                                                        .REQUEST_ID_HEADER,
                                                requestId
                                        );
                            }

                            return execution.execute(
                                    request,
                                    body
                            );
                        }
                );
    }
}