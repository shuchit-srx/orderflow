package com.orderflow.order.messaging;

import com.orderflow.order.outbox.OutboxMessage;

public interface OutboxBrokerPublisher {

    void publish(
            OutboxMessage message
    );
}