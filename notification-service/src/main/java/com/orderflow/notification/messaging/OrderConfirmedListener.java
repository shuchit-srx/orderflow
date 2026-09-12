package com.orderflow.notification.messaging;

import com.orderflow.notification.config.RabbitMqConfig;
import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.service.NotificationService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedListener {

    private final NotificationService
            notificationService;

    public OrderConfirmedListener(
            NotificationService notificationService
    ) {

        this.notificationService =
                notificationService;
    }

    @RabbitListener(
            queues = RabbitMqConfig.ORDER_CONFIRMED_QUEUE
    )
    public void onOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        notificationService
                .handleOrderConfirmed(
                        event
                );
    }
}