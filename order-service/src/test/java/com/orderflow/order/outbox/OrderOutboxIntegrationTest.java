package com.orderflow.order.outbox;

import com.orderflow.order.domain.outbox.OutboxStatus;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.repository.OutboxEventRepository;

import com.orderflow.order.service.OrderSagaService;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderOutboxIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderSagaService
            orderSagaService;

    @Autowired
    private OutboxEventRepository
            outboxEventRepository;

    @Test
    void confirmedOrderShouldCreatePendingOutboxEvent() {

        OrderResponse response =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_1,
                                UUID.randomUUID().toString(),
                                request()
                        );

        assertThat(
                response.status()
                        .name()
        )
                .isEqualTo(
                        "CONFIRMED"
                );

        assertThat(
                outboxEventRepository
                        .findAll()
        )
                .hasSize(
                        1
                );

        var outbox =
                outboxEventRepository
                        .findAll()
                        .getFirst();

        assertThat(
                outbox.getAggregateType()
        )
                .isEqualTo(
                        "ORDER"
                );

        assertThat(
                outbox.getAggregateId()
        )
                .isEqualTo(
                        response.id()
                );

        assertThat(
                outbox.getEventType()
        )
                .isEqualTo(
                        "ORDER_CONFIRMED"
                );

        assertThat(
                outbox.getEventVersion()
        )
                .isEqualTo(
                        1
                );

        assertThat(
                outbox.getStatus()
        )
                .isEqualTo(
                        OutboxStatus.PENDING
                );

        assertThat(
                outbox.getPayload()
        )
                .contains(
                        response.id()
                                .toString()
                );
    }

    private CreateOrderRequest request() {

        return new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(
                                IPHONE_ID,
                                1
                        )
                )
        );
    }
}