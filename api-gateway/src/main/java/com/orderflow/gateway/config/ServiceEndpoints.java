package com.orderflow.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "services")
public record ServiceEndpoints(
        URI user,
        URI inventory,
        URI order
) {
}