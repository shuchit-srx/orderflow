package com.orderflow.notification.messaging;

import com.orderflow.notification.config.RabbitMqConfig;
import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.service.NotificationEventProcessor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedListener {

    private final NotificationEventProcessor
            notificationEventProcessor;

    public OrderConfirmedListener(
            NotificationEventProcessor notificationEventProcessor
    ) {

        this.notificationEventProcessor =
                notificationEventProcessor;
    }

    @RabbitListener(
            queues =
                    RabbitMqConfig.ORDER_CONFIRMED_QUEUE
    )
    public void onOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        notificationEventProcessor
                .processOrderConfirmed(
                        event
                );
    }
}