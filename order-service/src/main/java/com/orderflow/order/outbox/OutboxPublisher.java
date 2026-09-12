package com.orderflow.order.outbox;

import com.orderflow.order.messaging.OutboxBrokerPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxPublisher.class
            );

    private final OutboxTransactionService
            transactionService;

    private final OutboxBrokerPublisher
            brokerPublisher;

    public OutboxPublisher(
            OutboxTransactionService transactionService,
            OutboxBrokerPublisher brokerPublisher
    ) {

        this.transactionService =
                transactionService;

        this.brokerPublisher =
                brokerPublisher;
    }

    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay-ms:1000}"
    )
    public void publishPendingEvents() {

        List<OutboxMessage> events =
                transactionService
                        .claimBatch();

        for (
                OutboxMessage event :
                events
        ) {

            publishOne(
                    event
            );
        }
    }

    private void publishOne(
            OutboxMessage event
    ) {

        try {

            brokerPublisher.publish(
                    event
            );

            transactionService
                    .markPublished(
                            event.id()
                    );

            log.info(
                    "Outbox event published eventId={} eventType={}",
                    event.id(),
                    event.eventType()
            );

        } catch (
                RuntimeException exception
        ) {

            log.warn(
                    "Outbox publishing failed eventId={} eventType={}",
                    event.id(),
                    event.eventType(),
                    exception
            );

            transactionService
                    .markPublishFailure(
                            event.id(),
                            exception
                    );
        }
    }
}