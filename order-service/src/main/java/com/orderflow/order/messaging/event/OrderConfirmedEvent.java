package com.orderflow.order.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String occurredAt,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount
) {

    public static OrderConfirmedEvent create(
            UUID orderId,
            UUID customerId,
            BigDecimal totalAmount
    ) {
        return new OrderConfirmedEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                1,
                Instant.now().toString(),
                orderId,
                customerId,
                totalAmount
        );
    }
}