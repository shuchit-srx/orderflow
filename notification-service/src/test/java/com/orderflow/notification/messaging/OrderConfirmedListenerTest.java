package com.orderflow.notification.messaging;

import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderConfirmedListenerTest {

    private NotificationService
            notificationService;

    private OrderConfirmedListener listener;

    @BeforeEach
    void setUp() {

        notificationService =
                mock(
                        NotificationService.class
                );

        listener =
                new OrderConfirmedListener(
                        notificationService
                );
    }

    @Test
    void shouldHandleOrderConfirmedEvent() {

        OrderConfirmedEvent event =
                new OrderConfirmedEvent(
                        UUID.randomUUID(),
                        "ORDER_CONFIRMED",
                        1,
                        "2026-09-12T06:00:00Z",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal(
                                "139998.00"
                        )
                );

        listener.onOrderConfirmed(
                event
        );

        verify(
                notificationService
        )
                .handleOrderConfirmed(
                        event
                );
    }
}