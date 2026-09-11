package com.orderflow.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(
        ServiceEndpoints.class
)
public class HttpClientConfig {

    @Bean
    public RestClient restClient() {

        return RestClient.builder()
                .build();
    }
}