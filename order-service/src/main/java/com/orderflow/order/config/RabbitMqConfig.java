package com.orderflow.order.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EVENTS_EXCHANGE =
            "orderflow.events";

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
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}