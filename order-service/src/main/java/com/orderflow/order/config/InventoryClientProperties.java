package com.orderflow.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "services.inventory")
public record InventoryClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}