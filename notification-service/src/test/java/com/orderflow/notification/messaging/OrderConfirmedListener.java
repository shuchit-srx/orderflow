package com.orderflow.notification.messaging;

import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.service.NotificationEventProcessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderConfirmedListenerTest {

    private NotificationEventProcessor
            processor;

    private OrderConfirmedListener
            listener;

    @BeforeEach
    void setUp() {

        processor =
                mock(
                        NotificationEventProcessor.class
                );

        listener =
                new OrderConfirmedListener(
                        processor
                );
    }

    @Test
    void shouldDelegateOrderConfirmedEvent() {

        OrderConfirmedEvent event =
                new OrderConfirmedEvent(
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

        listener.onOrderConfirmed(
                event
        );

        verify(
                processor
        )
                .processOrderConfirmed(
                        event
                );
    }
}