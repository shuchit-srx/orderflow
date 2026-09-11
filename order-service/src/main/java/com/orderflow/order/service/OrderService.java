package com.orderflow.order.service;

import com.orderflow.order.domain.Order;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderPageResponse;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.OrderNotFoundException;

import com.orderflow.order.repository.OrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.dto.InventoryProductResponse;

import com.orderflow.order.exception.DuplicateProductException;

import com.orderflow.order.service.model.ResolvedOrderItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.UUID;

@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    private final InventoryClient inventoryClient;

    private final OrderPersistenceService orderPersistenceService;

    public OrderService(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            OrderPersistenceService orderPersistenceService
    ) {

        this.orderRepository =
                orderRepository;

        this.inventoryClient =
                inventoryClient;

        this.orderPersistenceService =
                orderPersistenceService;
    }

    public OrderResponse createOrder(
            UUID customerId,
            CreateOrderRequest request
    ) {

        validateNoDuplicateProducts(
                request.items()
        );

        List<ResolvedOrderItem>
                resolvedItems =
                new ArrayList<>();

        for (CreateOrderItemRequest item
                : request.items()) {

            InventoryProductResponse product =
                    inventoryClient.getProduct(
                            item.productId()
                    );

            resolvedItems.add(
                    new ResolvedOrderItem(
                            product.id(),
                            product.sku(),
                            product.name(),
                            product.price(),
                            item.quantity()
                    )
            );
        }

        return orderPersistenceService
                .create(
                        customerId,
                        resolvedItems
                );
    }

    private void validateNoDuplicateProducts(
            List<CreateOrderItemRequest> items
    ) {

        Set<UUID> seen =
                new HashSet<>();

        for (CreateOrderItemRequest item : items) {

            if (!seen.add(
                    item.productId()
            )) {

                throw new DuplicateProductException(
                        item.productId()
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            UUID customerId,
            UUID orderId
    ) {

        Order order =
                orderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getOrders(
            UUID customerId,
            int page,
            int size
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.clamp(
                        size,
                        1,
                        MAX_PAGE_SIZE
                );

        Page<Order> orders =
                orderRepository
                        .findByCustomerId(
                                customerId,
                                PageRequest.of(
                                        safePage,
                                        safeSize,
                                        Sort.by(
                                                Sort.Direction.DESC,
                                                "createdAt"
                                        )
                                )
                        );

        return new OrderPageResponse(
                orders.getContent()
                        .stream()
                        .map(OrderResponse::from)
                        .toList(),

                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
        );
    }

    @Transactional
    public OrderResponse markInventoryReserved(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(orderId);

        order.markInventoryReserved();

        orderRepository.flush();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirmOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(orderId);

        order.confirm();

        orderRepository.flush();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(orderId);

        order.cancel();

        orderRepository.flush();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse failOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(orderId);

        order.fail();

        orderRepository.flush();

        return OrderResponse.from(order);
    }

    private Order getOrderForUpdate(
            UUID orderId
    ) {

        return orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                orderId
                        )
                );
    }
}