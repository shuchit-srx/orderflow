package com.orderflow.order.messaging;

import com.orderflow.order.config.OutboxPublisherProperties;

import com.orderflow.order.outbox.OutboxMessage;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RabbitOutboxBrokerPublisher
        implements OutboxBrokerPublisher {

    private final RabbitTemplate
            rabbitTemplate;

    private final OutboxPublisherProperties
            properties;

    public RabbitOutboxBrokerPublisher(
            RabbitTemplate rabbitTemplate,
            OutboxPublisherProperties properties
    ) {

        this.rabbitTemplate =
                rabbitTemplate;

        this.properties =
                properties;
    }

    @Override
    public void publish(
            OutboxMessage outboxMessage
    ) {

        Message message =
                MessageBuilder
                        .withBody(
                                outboxMessage
                                        .payload()
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .setContentType(
                                MessageProperties.CONTENT_TYPE_JSON
                        )
                        .setMessageId(
                                outboxMessage
                                        .id()
                                        .toString()
                        )
                        .setDeliveryMode(
                                MessageDeliveryMode.PERSISTENT
                        )
                        .setHeader(
                                "eventType",
                                outboxMessage.eventType()
                        )
                        .setHeader(
                                "eventVersion",
                                outboxMessage.eventVersion()
                        )
                        .build();

        rabbitTemplate.invoke(
                operations -> {

                    operations.send(
                            outboxMessage.exchangeName(),
                            outboxMessage.routingKey(),
                            message
                    );

                    operations
                            .waitForConfirmsOrDie(
                                    properties
                                            .confirmTimeout()
                                            .toMillis()
                            );

                    return null;
                }
        );
    }
}