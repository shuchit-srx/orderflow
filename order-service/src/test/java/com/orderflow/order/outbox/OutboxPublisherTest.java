package com.orderflow.order.outbox;

import com.orderflow.order.messaging.OutboxBrokerPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private OutboxTransactionService
            transactionService;

    private OutboxBrokerPublisher
            brokerPublisher;

    private OutboxPublisher
            publisher;

    @BeforeEach
    void setUp() {

        transactionService =
                mock(
                        OutboxTransactionService.class
                );

        brokerPublisher =
                mock(
                        OutboxBrokerPublisher.class
                );

        publisher =
                new OutboxPublisher(
                        transactionService,
                        brokerPublisher
                );
    }

    @Test
    void shouldMarkEventPublishedAfterBrokerSuccess() {

        OutboxMessage event =
                event();

        when(
                transactionService
                        .claimBatch()
        )
                .thenReturn(
                        List.of(
                                event
                        )
                );

        publisher.publishPendingEvents();

        verify(
                brokerPublisher
        )
                .publish(
                        event
                );

        verify(
                transactionService
        )
                .markPublished(
                        event.id()
                );
    }

    @Test
    void shouldScheduleRetryAfterBrokerFailure() {

        OutboxMessage event =
                event();

        RuntimeException failure =
                new RuntimeException(
                        "RabbitMQ unavailable"
                );

        when(
                transactionService
                        .claimBatch()
        )
                .thenReturn(
                        List.of(
                                event
                        )
                );

        doThrow(
                failure
        )
                .when(
                        brokerPublisher
                )
                .publish(
                        event
                );

        publisher.publishPendingEvents();

        verify(
                transactionService
        )
                .markPublishFailure(
                        event.id(),
                        failure
                );
    }

    private OutboxMessage event() {

        return new OutboxMessage(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                1,
                "orderflow.events",
                "order.confirmed.v1",
                """
                {
                  "eventType":"ORDER_CONFIRMED"
                }
                """
        );
    }
}