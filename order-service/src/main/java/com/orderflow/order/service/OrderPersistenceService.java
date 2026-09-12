package com.orderflow.order.service;

import com.orderflow.order.domain.Order;

import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.repository.OrderRepository;

import com.orderflow.order.service.model.ResolvedOrderItem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;

    public OrderPersistenceService(
            OrderRepository orderRepository
    ) {

        this.orderRepository =
                orderRepository;
    }

    @Transactional
    public OrderResponse create(
            UUID customerId,
            String idempotencyKey,
            String requestHash,
            List<ResolvedOrderItem> items
    ) {

        Order order =
                new Order(customerId,
                        idempotencyKey,
                        requestHash);

        for (ResolvedOrderItem item : items) {

            order.addItem(
                    item.productId(),
                    item.sku(),
                    item.productName(),
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

    @Transactional
    public OrderResponse createIdempotent(
            UUID customerId,
            String idempotencyKey,
            String requestHash,
            List<ResolvedOrderItem> resolvedItems
    ) {

        Order order =
                new Order(
                        customerId,
                        idempotencyKey,
                        requestHash
                );

        for (
                ResolvedOrderItem item :
                resolvedItems
        ) {

            order.addItem(
                    item.productId(),
                    item.sku(),
                    item.productName(),
                    item.unitPrice(),
                    item.quantity()
            );
        }

        Order saved =
                orderRepository.saveAndFlush(
                        order
                );

        return OrderResponse.from(
                saved
        );
    }
}