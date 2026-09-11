package com.orderflow.order.service;

import com.orderflow.order.client.InventoryClient;

import com.orderflow.order.client.dto.ReserveInventoryItemRequest;
import com.orderflow.order.client.dto.ReserveInventoryRequest;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.InventoryReservationRejectedException;
import com.orderflow.order.exception.SagaCompensationException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderSagaService {

    private final OrderService orderService;

    private final InventoryClient inventoryClient;

    public OrderSagaService(
            OrderService orderService,
            InventoryClient inventoryClient
    ) {

        this.orderService =
                orderService;

        this.inventoryClient =
                inventoryClient;
    }

    public OrderResponse placeOrder(
            UUID customerId,
            CreateOrderRequest request
    ) {

        OrderResponse createdOrder =
                orderService.createOrder(
                        customerId,
                        request
                );

        UUID orderId =
                createdOrder.id();

        ReserveInventoryRequest reservationRequest =
                buildReservationRequest(
                        orderId,
                        request
                );

        boolean reservationMayExist =
                false;

        try {

            reservationMayExist =
                    true;

            inventoryClient
                    .reserveInventory(
                            reservationRequest
                    );

            orderService
                    .markInventoryReserved(
                            orderId
                    );

            inventoryClient
                    .confirmReservation(
                            orderId
                    );

            return orderService
                    .confirmOrder(
                            orderId
                    );

        } catch (
                InventoryReservationRejectedException exception
        ) {

            failOrderBestEffort(
                    orderId,
                    exception
            );

            throw exception;

        } catch (
                RuntimeException originalFailure
        ) {

            RuntimeException compensationFailure =
                    null;

            if (reservationMayExist) {

                try {

                    inventoryClient
                            .releaseReservation(
                                    orderId
                            );

                } catch (
                        RuntimeException releaseFailure
                ) {

                    compensationFailure =
                            releaseFailure;
                }
            }

            try {

                orderService
                        .failOrder(
                                orderId
                        );

            } catch (
                    RuntimeException orderFailure
            ) {

                if (
                        compensationFailure == null
                ) {

                    compensationFailure =
                            orderFailure;

                } else {

                    compensationFailure
                            .addSuppressed(
                                    orderFailure
                            );
                }
            }

            if (
                    compensationFailure != null
            ) {

                throw new SagaCompensationException(
                        orderId,
                        originalFailure,
                        compensationFailure
                );
            }

            throw originalFailure;
        }
    }

    private ReserveInventoryRequest
    buildReservationRequest(
            UUID orderId,
            CreateOrderRequest request
    ) {

        List<ReserveInventoryItemRequest>
                reservationItems =
                request
                        .items()
                        .stream()
                        .map(
                                this::toReservationItem
                        )
                        .toList();

        return new ReserveInventoryRequest(
                orderId,
                reservationItems
        );
    }

    private ReserveInventoryItemRequest
    toReservationItem(
            CreateOrderItemRequest item
    ) {

        return new ReserveInventoryItemRequest(
                item.productId(),
                item.quantity()
        );
    }

    private void failOrderBestEffort(
            UUID orderId,
            RuntimeException originalFailure
    ) {

        try {

            orderService
                    .failOrder(
                            orderId
                    );

        } catch (
                RuntimeException failure
        ) {

            originalFailure
                    .addSuppressed(
                            failure
                    );
        }
    }
}