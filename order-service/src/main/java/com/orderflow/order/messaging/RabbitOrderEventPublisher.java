package com.orderflow.order.messaging;

import com.orderflow.order.config.RabbitMqConfig;
import com.orderflow.order.messaging.event.OrderConfirmedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

import org.springframework.stereotype.Component;

@Component
public class RabbitOrderEventPublisher
        implements OrderEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RabbitOrderEventPublisher.class
            );

    private final AmqpTemplate amqpTemplate;

    public RabbitOrderEventPublisher(
            AmqpTemplate amqpTemplate
    ) {
        this.amqpTemplate =
                amqpTemplate;
    }

    @Override
    public void publishOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        try {

            amqpTemplate.convertAndSend(
                    RabbitMqConfig.ORDER_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_CONFIRMED_ROUTING_KEY,
                    event
            );

            log.info(
                    "Published ORDER_CONFIRMED event eventId={} orderId={}",
                    event.eventId(),
                    event.orderId()
            );

        } catch (AmqpException exception) {

            log.error(
                    "Failed to publish ORDER_CONFIRMED event eventId={} orderId={}",
                    event.eventId(),
                    event.orderId(),
                    exception
            );
        }
    }
}