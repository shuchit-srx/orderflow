package com.orderflow.order.service;

import com.orderflow.order.client.dto.ReserveInventoryRequest;

import com.orderflow.order.domain.OrderStatus;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.InventoryReservationRejectedException;
import com.orderflow.order.exception.InventoryServiceUnavailableException;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderSagaServiceIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderSagaService orderSagaService;

    @Test
    void successfulSagaShouldConfirmOrder() {

        OrderResponse response =
                orderSagaService
                        .placeOrder(
                                CUSTOMER_1,
                                request()
                        );

        assertThat(
                response.status()
        )
                .isEqualTo(
                        OrderStatus.CONFIRMED
                );

        assertThat(
                orderStatus(
                        response.id()
                )
        )
                .isEqualTo(
                        "CONFIRMED"
                );

        verify(
                inventoryClient
        )
                .reserveInventory(
                        any(
                                ReserveInventoryRequest.class
                        )
                );

        verify(
                inventoryClient
        )
                .confirmReservation(
                        response.id()
                );

        verify(
                inventoryClient,
                never()
        )
                .releaseReservation(
                        response.id()
                );
    }

    @Test
    void rejectedReservationShouldFailOrder() {

        doAnswer(
                invocation -> {

                    ReserveInventoryRequest request =
                            invocation.getArgument(
                                    0
                            );

                    throw new InventoryReservationRejectedException(
                            request.orderId()
                    );
                }
        )
                .when(
                        inventoryClient
                )
                .reserveInventory(
                        any(
                                ReserveInventoryRequest.class
                        )
                );

        assertThatThrownBy(
                () ->
                        orderSagaService
                                .placeOrder(
                                        CUSTOMER_1,
                                        request()
                                )
        )
                .isInstanceOf(
                        InventoryReservationRejectedException.class
                );

        String status =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM orders
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                        String.class
                );

        assertThat(status)
                .isEqualTo(
                        "FAILED"
                );
    }

    @Test
    void failureAfterReservationShouldCompensate() {

        doThrow(
                new InventoryServiceUnavailableException(
                        new RuntimeException(
                                "simulated failure"
                        )
                )
        )
                .when(
                        inventoryClient
                )
                .confirmReservation(
                        any(UUID.class)
                );

        assertThatThrownBy(
                () ->
                        orderSagaService
                                .placeOrder(
                                        CUSTOMER_1,
                                        request()
                                )
        )
                .isInstanceOf(
                        InventoryServiceUnavailableException.class
                );

        verify(
                inventoryClient
        )
                .releaseReservation(
                        any(UUID.class)
                );

        String status =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM orders
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                        String.class
                );

        assertThat(status)
                .isEqualTo(
                        "FAILED"
                );
    }

    private CreateOrderRequest request() {

        return new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(
                                IPHONE_ID,
                                2
                        )
                )
        );
    }
}