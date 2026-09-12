package com.orderflow.order.outbox;

import com.orderflow.order.config.OutboxPublisherProperties;
import com.orderflow.order.domain.outbox.OutboxEvent;
import com.orderflow.order.domain.outbox.OutboxStatus;
import com.orderflow.order.repository.OutboxEventRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxTransactionService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherProperties properties;

    public OutboxTransactionService(
            OutboxEventRepository outboxEventRepository,
            OutboxPublisherProperties properties
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.properties =
                properties;
    }

    @Transactional
    public List<OutboxMessage> claimBatch() {

        Instant now =
                Instant.now();

        Instant staleBefore =
                now.minus(
                        properties.staleProcessingAfter()
                );

        outboxEventRepository
                .releaseStaleEvents(
                        OutboxStatus.PROCESSING,
                        OutboxStatus.PENDING,
                        staleBefore,
                        now
                );

        List<OutboxEvent> events =
                outboxEventRepository
                        .findBatchForUpdate(
                                OutboxStatus.PENDING,
                                now,
                                PageRequest.of(
                                        0,
                                        properties.batchSize()
                                )
                        );

        for (OutboxEvent event : events) {
            event.claim(now);
        }

        return events
                .stream()
                .map(
                        OutboxMessage::from
                )
                .toList();
    }

    @Transactional
    public void markPublished(
            UUID eventId
    ) {

        OutboxEvent event =
                outboxEventRepository
                        .findByIdForUpdate(
                                eventId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Outbox event not found: "
                                                        + eventId
                                        )
                        );

        if (
                event.getStatus()
                        == OutboxStatus.PUBLISHED
        ) {
            return;
        }

        event.markPublished(
                Instant.now()
        );
    }

    @Transactional
    public void markPublishFailure(
            UUID eventId,
            Throwable failure
    ) {

        OutboxEvent event =
                outboxEventRepository
                        .findByIdForUpdate(
                                eventId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Outbox event not found: "
                                                        + eventId
                                        )
                        );

        int nextAttemptNumber =
                event.getAttempts()
                        + 1;

        String error =
                failure.getClass()
                        .getSimpleName()
                        + ": "
                        + failure.getMessage();

        if (
                nextAttemptNumber
                        >= properties.maxAttempts()
        ) {

            event.markFailed(
                    error
            );

            return;
        }

        Duration backoff =
                calculateBackoff(
                        nextAttemptNumber
                );

        event.scheduleRetry(
                Instant.now()
                        .plus(
                                backoff
                        ),
                error
        );
    }

    private Duration calculateBackoff(
            int attempt
    ) {

        long multiplier =
                1L << Math.min(
                        attempt - 1,
                        20
                );

        long requestedMillis =
                properties
                        .baseBackoff()
                        .toMillis()
                        * multiplier;

        long maxMillis =
                properties
                        .maxBackoff()
                        .toMillis();

        return Duration.ofMillis(
                Math.min(
                        requestedMillis,
                        maxMillis
                )
        );
    }
}