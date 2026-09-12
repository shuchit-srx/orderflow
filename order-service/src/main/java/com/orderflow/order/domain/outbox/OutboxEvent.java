package com.orderflow.order.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "event_version",
            nullable = false
    )
    private int eventVersion;

    @Column(
            name = "exchange_name",
            nullable = false,
            length = 200
    )
    private String exchangeName;

    @Column(
            name = "routing_key",
            nullable = false,
            length = 200
    )
    private String routingKey;

    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private OutboxStatus status;

    @Column(
            name = "attempts",
            nullable = false
    )
    private int attempts;

    @Column(
            name = "next_attempt_at",
            nullable = false
    )
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            String exchangeName,
            String routingKey,
            String payload
    ) {

        this.id =
                id;

        this.aggregateType =
                aggregateType;

        this.aggregateId =
                aggregateId;

        this.eventType =
                eventType;

        this.eventVersion =
                eventVersion;

        this.exchangeName =
                exchangeName;

        this.routingKey =
                routingKey;

        this.payload =
                payload;

        this.status =
                OutboxStatus.PENDING;

        this.attempts =
                0;

        this.nextAttemptAt =
                Instant.now();
    }

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            String exchangeName,
            String routingKey,
            String payload
    ) {

        return new OutboxEvent(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                exchangeName,
                routingKey,
                payload
        );
    }

    public void claim(
            Instant now
    ) {

        if (
                status != OutboxStatus.PENDING
        ) {

            throw new IllegalStateException(
                    "Only PENDING outbox events can be claimed"
            );
        }

        status =
                OutboxStatus.PROCESSING;

        lockedAt =
                now;
    }

    public void markPublished(
            Instant now
    ) {

        status =
                OutboxStatus.PUBLISHED;

        publishedAt =
                now;

        lockedAt =
                null;

        lastError =
                null;
    }

    public void scheduleRetry(
            Instant nextAttempt,
            String error
    ) {

        attempts++;

        status =
                OutboxStatus.PENDING;

        nextAttemptAt =
                nextAttempt;

        lockedAt =
                null;

        lastError =
                truncate(error);
    }

    public void markFailed(
            String error
    ) {

        attempts++;

        status =
                OutboxStatus.FAILED;

        lockedAt =
                null;

        lastError =
                truncate(error);
    }

    private String truncate(
            String value
    ) {

        if (value == null) {
            return null;
        }

        int maxLength =
                4000;

        if (
                value.length()
                        <= maxLength
        ) {
            return value;
        }

        return value.substring(
                0,
                maxLength
        );
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}