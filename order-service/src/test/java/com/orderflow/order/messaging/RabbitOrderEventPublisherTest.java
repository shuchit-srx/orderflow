package com.orderflow.order.messaging;

import com.orderflow.order.config.RabbitMqConfig;
import com.orderflow.order.messaging.event.OrderConfirmedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitOrderEventPublisherTest {

    private AmqpTemplate amqpTemplate;

    private RabbitOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {

        amqpTemplate =
                mock(
                        AmqpTemplate.class
                );

        publisher =
                new RabbitOrderEventPublisher(
                        amqpTemplate
                );
    }

    @Test
    void shouldPublishOrderConfirmedEvent() {

        OrderConfirmedEvent event =
                OrderConfirmedEvent.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal(
                                "139998.00"
                        )
                );

        publisher.publishOrderConfirmed(
                event
        );

        verify(
                amqpTemplate
        )
                .convertAndSend(
                        RabbitMqConfig.ORDER_EVENTS_EXCHANGE,
                        RabbitMqConfig.ORDER_CONFIRMED_ROUTING_KEY,
                        event
                );
    }
}