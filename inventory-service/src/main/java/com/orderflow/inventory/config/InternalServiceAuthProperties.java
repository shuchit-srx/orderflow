package com.orderflow.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.internal")
public record InternalServiceAuthProperties(
        String token
) {
}