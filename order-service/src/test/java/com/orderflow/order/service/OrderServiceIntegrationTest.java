package com.orderflow.order.service;

import com.orderflow.order.domain.OrderStatus;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.InvalidOrderStateException;
import com.orderflow.order.exception.OrderNotFoundException;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions
        .assertThat;

import static org.assertj.core.api.Assertions
        .assertThatThrownBy;

class OrderServiceIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    private CreateOrderRequest request() {

        return new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(
                                IPHONE_ID,
                                "IPHONE-15",
                                "iPhone 15",
                                new BigDecimal(
                                        "69999.00"
                                ),
                                2
                        )
                )
        );
    }

    @Test
    void shouldCreateOrderForAuthenticatedCustomer() {

        OrderResponse response =
                orderService.createOrder(
                        CUSTOMER_1,
                        request()
                );

        assertThat(response.id())
                .isNotNull();

        assertThat(response.customerId())
                .isEqualTo(CUSTOMER_1);

        assertThat(response.status())
                .isEqualTo(
                        OrderStatus.CREATED
                );

        assertThat(response.totalAmount())
                .isEqualByComparingTo(
                        "139998.00"
                );

        assertThat(orderCount())
                .isEqualTo(1);
    }

    @Test
    void customerShouldNotReadAnotherCustomersOrder() {

        OrderResponse created =
                orderService.createOrder(
                        CUSTOMER_1,
                        request()
                );

        assertThatThrownBy(() ->
                orderService.getOrder(
                        CUSTOMER_2,
                        created.id()
                )
        )
                .isInstanceOf(
                        OrderNotFoundException.class
                );
    }

    @Test
    void shouldCompleteValidOrderFlow() {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                request()
                        )
                        .id();

        OrderResponse reserved =
                orderService
                        .markInventoryReserved(
                                orderId
                        );

        assertThat(reserved.status())
                .isEqualTo(
                        OrderStatus.INVENTORY_RESERVED
                );

        OrderResponse confirmed =
                orderService.confirmOrder(
                        orderId
                );

        assertThat(confirmed.status())
                .isEqualTo(
                        OrderStatus.CONFIRMED
                );

        assertThat(orderStatus(orderId))
                .isEqualTo("CONFIRMED");
    }

    @Test
    void repeatedTransitionShouldBeIdempotent() {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                request()
                        )
                        .id();

        orderService
                .markInventoryReserved(
                        orderId
                );

        orderService
                .markInventoryReserved(
                        orderId
                );

        orderService
                .confirmOrder(
                        orderId
                );

        orderService
                .confirmOrder(
                        orderId
                );

        assertThat(orderStatus(orderId))
                .isEqualTo("CONFIRMED");
    }

    @Test
    void shouldRejectIllegalTransition() {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                request()
                        )
                        .id();

        orderService
                .markInventoryReserved(
                        orderId
                );

        orderService
                .confirmOrder(
                        orderId
                );

        assertThatThrownBy(() ->
                orderService.cancelOrder(
                        orderId
                )
        )
                .isInstanceOf(
                        InvalidOrderStateException.class
                );

        assertThat(orderStatus(orderId))
                .isEqualTo("CONFIRMED");
    }

    @Test
    void shouldCancelCreatedOrder() {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                request()
                        )
                        .id();

        OrderResponse response =
                orderService
                        .cancelOrder(
                                orderId
                        );

        assertThat(response.status())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );
    }
}