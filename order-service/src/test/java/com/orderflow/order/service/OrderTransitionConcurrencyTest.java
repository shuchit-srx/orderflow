package com.orderflow.order.service;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;

import com.orderflow.order.exception.InvalidOrderStateException;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions
        .assertThat;

class OrderTransitionConcurrencyTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void confirmAndCancelRaceShouldRemainConsistent()
            throws Exception {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                new CreateOrderRequest(
                                        List.of(
                                                new CreateOrderItemRequest(
                                                        IPHONE_ID,
                                                        "IPHONE-15",
                                                        "iPhone 15",
                                                        new BigDecimal(
                                                                "69999.00"
                                                        ),
                                                        1
                                                )
                                        )
                                )
                        )
                        .id();

        orderService
                .markInventoryReserved(
                        orderId
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            Future<Boolean> confirm =
                    executor.submit(() -> {

                        ready.countDown();
                        start.await();

                        try {

                            orderService
                                    .confirmOrder(
                                            orderId
                                    );

                            return true;

                        } catch (
                                InvalidOrderStateException
                                        exception
                        ) {

                            return false;
                        }
                    });

            Future<Boolean> cancel =
                    executor.submit(() -> {

                        ready.countDown();
                        start.await();

                        try {

                            orderService
                                    .cancelOrder(
                                            orderId
                                    );

                            return true;

                        } catch (
                                InvalidOrderStateException
                                        exception
                        ) {

                            return false;
                        }
                    });

            ready.await();

            start.countDown();

            boolean confirmSucceeded =
                    confirm.get();

            boolean cancelSucceeded =
                    cancel.get();

            assertThat(
                    confirmSucceeded
                            ^ cancelSucceeded
            )
                    .isTrue();

            String finalStatus =
                    orderStatus(orderId);

            assertThat(finalStatus)
                    .isIn(
                            "CONFIRMED",
                            "CANCELLED"
                    );

        } finally {

            executor.shutdownNow();
        }
    }
}