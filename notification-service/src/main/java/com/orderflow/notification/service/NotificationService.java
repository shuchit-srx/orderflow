package com.orderflow.notification.service;

import com.orderflow.notification.messaging.event.OrderConfirmedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationService.class
            );

    public void handleOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        log.info(
                "Order confirmation notification: eventId={} orderId={} customerId={} totalAmount={}",
                event.eventId(),
                event.orderId(),
                event.customerId(),
                event.totalAmount()
        );
    }
}