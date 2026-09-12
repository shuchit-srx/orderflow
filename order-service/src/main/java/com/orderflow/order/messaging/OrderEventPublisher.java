package com.orderflow.order.messaging;

import com.orderflow.order.messaging.event.OrderConfirmedEvent;

public interface OrderEventPublisher {

    void publishOrderConfirmed(
            OrderConfirmedEvent event
    );
}