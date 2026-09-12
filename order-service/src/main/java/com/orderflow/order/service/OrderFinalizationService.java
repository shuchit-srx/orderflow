package com.orderflow.order.service;

import com.orderflow.order.config.RabbitMqConfig;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.domain.outbox.OutboxEvent;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.messaging.event.OrderConfirmedEvent;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.repository.OutboxEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class OrderFinalizationService {

    private static final String AGGREGATE_TYPE =
            "ORDER";

    private static final String EVENT_TYPE =
            "ORDER_CONFIRMED";

    private static final int EVENT_VERSION =
            1;

    private final OrderRepository
            orderRepository;

    private final OutboxEventRepository
            outboxEventRepository;

    private final JsonMapper
            jsonMapper;

    public OrderFinalizationService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper
    ) {

        this.orderRepository =
                orderRepository;

        this.outboxEventRepository =
                outboxEventRepository;

        this.jsonMapper =
                jsonMapper;
    }

    @Transactional
    public OrderResponse confirmOrderAndCreateOutbox(
            UUID orderId
    ) {

        Order order =
                orderRepository
                        .findByIdForUpdate(
                                orderId
                        )
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                orderId
                                        )
                        );

        /*
         * If the order is already CONFIRMED,
         * don't transition it again.
         *
         * We still check whether the matching
         * outbox event exists. This is useful
         * for recovery and older Phase-8 orders.
         */
        if (
                order.getStatus()
                        != OrderStatus.CONFIRMED
        ) {

            order.confirm();
        }

        createOutboxIfMissing(
                order
        );

        return OrderResponse.from(
                order
        );
    }

    private void createOutboxIfMissing(
            Order order
    ) {

        boolean alreadyExists =
                outboxEventRepository
                        .existsByAggregateTypeAndAggregateIdAndEventTypeAndEventVersion(
                                AGGREGATE_TYPE,
                                order.getId(),
                                EVENT_TYPE,
                                EVENT_VERSION
                        );

        if (alreadyExists) {
            return;
        }

        OrderConfirmedEvent event =
                OrderConfirmedEvent.create(
                        order.getId(),
                        order.getCustomerId(),
                        order.getTotalAmount()
                );

        String payload;

        try {

            payload =
                    jsonMapper
                            .writeValueAsString(
                                    event
                            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not serialize ORDER_CONFIRMED event",
                    exception
            );
        }

        OutboxEvent outboxEvent =
                OutboxEvent.pending(
                        event.eventId(),
                        AGGREGATE_TYPE,
                        order.getId(),
                        EVENT_TYPE,
                        EVENT_VERSION,
                        RabbitMqConfig.ORDER_EVENTS_EXCHANGE,
                        RabbitMqConfig.ORDER_CONFIRMED_ROUTING_KEY,
                        payload
                );

        outboxEventRepository.save(
                outboxEvent
        );
    }
}