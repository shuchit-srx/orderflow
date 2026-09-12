package com.orderflow.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EVENTS_EXCHANGE =
            "orderflow.events";

    public static final String ORDER_CONFIRMED_QUEUE =
            "notification.order-confirmed.v1";

    public static final String ORDER_CONFIRMED_ROUTING_KEY =
            "order.confirmed.v1";

    @Bean
    public TopicExchange orderEventsExchange() {

        return new TopicExchange(
                ORDER_EVENTS_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue orderConfirmedQueue() {

        return QueueBuilder
                .durable(
                        ORDER_CONFIRMED_QUEUE
                )
                .build();
    }

    @Bean
    public Binding orderConfirmedBinding(
            Queue orderConfirmedQueue,
            TopicExchange orderEventsExchange
    ) {

        return BindingBuilder
                .bind(
                        orderConfirmedQueue
                )
                .to(
                        orderEventsExchange
                )
                .with(
                        ORDER_CONFIRMED_ROUTING_KEY
                );
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {

        return new JacksonJsonMessageConverter();
    }
}