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

import java.util.UUID;

@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    public OrderService(
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request
    ) {

        Order order =
                new Order(
                        request.customerId()
                );

        for (CreateOrderItemRequest item : request.items()) {

            order.addItem(
                    item.productId(),
                    item.sku().trim(),
                    item.productName().trim(),
                    item.unitPrice(),
                    item.quantity()
            );
        }

        Order saved =
                orderRepository.saveAndFlush(
                        order
                );

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            UUID orderId
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getOrders(
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
                orderRepository.findAll(
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
}