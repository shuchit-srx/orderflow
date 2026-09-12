package com.orderflow.order.service;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.IdempotencyKeyConflictException;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdempotencyIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderSagaService
            orderSagaService;

    @Test
    void sameKeyAndSameRequestShouldReturnSameOrder() {

        String key =
                UUID.randomUUID()
                        .toString();

        CreateOrderRequest request =
                request(
                        IPHONE_ID,
                        1
                );

        OrderResponse first =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_1,
                                key,
                                request
                        );

        OrderResponse second =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_1,
                                key,
                                request
                        );

        assertThat(
                second.id()
        )
                .isEqualTo(
                        first.id()
                );

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        WHERE customer_id = ?
                          AND idempotency_key = ?
                        """,
                        Integer.class,
                        CUSTOMER_1,
                        key
                );

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentRequestShouldFail() {

        String key =
                UUID.randomUUID()
                        .toString();

        orderSagaService.placeOrder(
                CUSTOMER_1,
                key,
                request(
                        IPHONE_ID,
                        1
                )
        );

        assertThatThrownBy(
                () ->
                        orderSagaService
                                .placeOrder(
                                        CUSTOMER_1,
                                        key,
                                        request(
                                                IPHONE_ID,
                                                2
                                        )
                                )
        )
                .isInstanceOf(
                        IdempotencyKeyConflictException.class
                );
    }

    @Test
    void sameKeyCanBeUsedByDifferentCustomers() {

        String key =
                UUID.randomUUID()
                        .toString();

        OrderResponse first =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_1,
                                key,
                                request(
                                        IPHONE_ID,
                                        1
                                )
                        );

        OrderResponse second =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_2,
                                key,
                                request(
                                        IPHONE_ID,
                                        1
                                )
                        );

        assertThat(
                first.id()
        )
                .isNotEqualTo(
                        second.id()
                );
    }

    private CreateOrderRequest request(
            UUID productId,
            int quantity
    ) {

        return new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(
                                productId,
                                quantity
                        )
                )
        );
    }
}