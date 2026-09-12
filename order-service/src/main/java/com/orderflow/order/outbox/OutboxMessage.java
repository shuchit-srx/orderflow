package com.orderflow.order.outbox;

import com.orderflow.order.domain.outbox.OutboxEvent;

import java.util.UUID;

public record OutboxMessage(

        UUID id,

        String eventType,

        int eventVersion,

        String exchangeName,

        String routingKey,

        String payload

) {

    public static OutboxMessage from(
            OutboxEvent event
    ) {

        return new OutboxMessage(
                event.getId(),
                event.getEventType(),
                event.getEventVersion(),
                event.getExchangeName(),
                event.getRoutingKey(),
                event.getPayload()
        );
    }
}