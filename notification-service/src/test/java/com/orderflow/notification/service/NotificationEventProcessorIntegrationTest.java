package com.orderflow.notification.service;

import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.repository.ProcessedEventRepository;
import com.orderflow.notification.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationEventProcessorIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private NotificationEventProcessor
            processor;

    @Autowired
    private ProcessedEventRepository
            processedEventRepository;

    @MockitoBean
    private NotificationService
            notificationService;

    @Test
    void duplicateEventShouldBeProcessedOnlyOnce() {

        OrderConfirmedEvent event =
                event();

        processor.processOrderConfirmed(
                event
        );

        processor.processOrderConfirmed(
                event
        );

        verify(
                notificationService,
                times(1)
        )
                .handleOrderConfirmed(
                        event
                );

        assertThat(
                processedEventRepository.count()
        )
                .isEqualTo(
                        1
                );

        assertThat(
                processedEventRepository
                        .existsById(
                                event.eventId()
                        )
        )
                .isTrue();
    }

    @Test
    void failedProcessingShouldRollbackProcessedEventMarker() {

        OrderConfirmedEvent event =
                event();

        doThrow(
                new RuntimeException(
                        "Notification provider unavailable"
                )
        )
                .when(
                        notificationService
                )
                .handleOrderConfirmed(
                        event
                );

        assertThatThrownBy(
                () ->
                        processor
                                .processOrderConfirmed(
                                        event
                                )
        )
                .isInstanceOf(
                        RuntimeException.class
                );

        assertThat(
                processedEventRepository
                        .existsById(
                                event.eventId()
                        )
        )
                .isFalse();

        reset(
                notificationService
        );

        processor.processOrderConfirmed(
                event
        );

        assertThat(
                processedEventRepository
                        .existsById(
                                event.eventId()
                        )
        )
                .isTrue();

        verify(
                notificationService,
                times(1)
        )
                .handleOrderConfirmed(
                        event
                );
    }

    private OrderConfirmedEvent event() {

        return new OrderConfirmedEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                1,
                Instant.now()
                        .toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal(
                        "69999.00"
                )
        );
    }
}