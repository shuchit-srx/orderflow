package com.orderflow.notification.service;

import com.orderflow.notification.messaging.event.OrderConfirmedEvent;
import com.orderflow.notification.repository.ProcessedEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationEventProcessor.class
            );

    private final ProcessedEventRepository
            processedEventRepository;

    private final NotificationService
            notificationService;

    public NotificationEventProcessor(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService
    ) {

        this.processedEventRepository =
                processedEventRepository;

        this.notificationService =
                notificationService;
    }

    @Transactional
    public void processOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        if (
                event == null
                        || event.eventId() == null
        ) {

            throw new IllegalArgumentException(
                    "ORDER_CONFIRMED eventId must be present"
            );
        }

        int inserted =
                processedEventRepository
                        .insertIfAbsent(
                                event.eventId(),
                                event.eventType()
                        );

        if (inserted == 0) {

            log.info(
                    "Ignoring duplicate event eventId={} eventType={}",
                    event.eventId(),
                    event.eventType()
            );

            return;
        }

        notificationService
                .handleOrderConfirmed(
                        event
                );

        log.info(
                "Processed event eventId={} eventType={}",
                event.eventId(),
                event.eventType()
        );
    }
}