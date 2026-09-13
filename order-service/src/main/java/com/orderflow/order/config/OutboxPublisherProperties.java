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

    public OutboxPublisherProperties {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "outbox.publisher.batch-size must be greater than 0"
            );
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "outbox.publisher.max-attempts must be greater than 0"
            );
        }

        if (staleProcessingAfter == null
                || staleProcessingAfter.isZero()
                || staleProcessingAfter.isNegative()) {

            throw new IllegalArgumentException(
                    "outbox.publisher.stale-processing-after must be greater than 0"
            );
        }

        if (baseBackoff == null
                || baseBackoff.isNegative()) {

            throw new IllegalArgumentException(
                    "outbox.publisher.base-backoff must not be negative"
            );
        }

        if (maxBackoff == null
                || maxBackoff.isNegative()) {

            throw new IllegalArgumentException(
                    "outbox.publisher.max-backoff must not be negative"
            );
        }

        if (confirmTimeout == null
                || confirmTimeout.isZero()
                || confirmTimeout.isNegative()) {

            throw new IllegalArgumentException(
                    "outbox.publisher.confirm-timeout must be greater than 0"
            );
        }

        if (maxBackoff.compareTo(baseBackoff) < 0) {
            throw new IllegalArgumentException(
                    "outbox.publisher.max-backoff must be greater than or equal to base-backoff"
            );
        }
    }
}