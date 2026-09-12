package com.orderflow.order.repository;

import com.orderflow.order.domain.outbox.OutboxEvent;
import com.orderflow.order.domain.outbox.OutboxStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    boolean existsByAggregateTypeAndAggregateIdAndEventTypeAndEventVersion(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT event
            FROM OutboxEvent event
            WHERE event.status = :status
              AND event.nextAttemptAt <= :now
            ORDER BY event.createdAt ASC
            """
    )
    List<OutboxEvent> findBatchForUpdate(
            @Param("status")
            OutboxStatus status,

            @Param("now")
            Instant now,

            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT event
            FROM OutboxEvent event
            WHERE event.id = :eventId
            """
    )
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("eventId")
            UUID eventId
    );

    @Modifying
    @Query(
            """
            UPDATE OutboxEvent event
            SET event.status = :pendingStatus,
                event.lockedAt = NULL,
                event.nextAttemptAt = :now,
                event.updatedAt = :now
            WHERE event.status = :processingStatus
              AND event.lockedAt < :staleBefore
            """
    )
    void releaseStaleEvents(
            @Param("processingStatus")
            OutboxStatus processingStatus,

            @Param("pendingStatus")
            OutboxStatus pendingStatus,

            @Param("staleBefore")
            Instant staleBefore,

            @Param("now")
            Instant now
    );
}