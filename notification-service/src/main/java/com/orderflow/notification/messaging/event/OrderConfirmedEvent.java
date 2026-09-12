package com.orderflow.notification.messaging.event;

import java.math.BigDecimal;
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
}