package com.orderflow.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "outbox.publisher"
)
public record OutboxPublisherProperties(

        boolean enabled,

        int batchSize,

        int maxAttempts,

        Duration staleProcessingAfter,

        Duration baseBackoff,

        Duration maxBackoff,

        Duration confirmTimeout

) {
}