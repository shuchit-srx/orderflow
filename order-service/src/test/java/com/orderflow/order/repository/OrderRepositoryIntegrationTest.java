package com.orderflow.order.repository;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions
        .assertThat;

class OrderRepositoryIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @Transactional
    void shouldPersistOrderWithItemsAndTotal() {

        Order order =
                new Order(CUSTOMER_1);

        order.addItem(
                IPHONE_ID,
                "IPHONE-15",
                "iPhone 15",
                new BigDecimal("69999.00"),
                2
        );

        order.addItem(
                PIXEL_ID,
                "PIXEL-9",
                "Google Pixel 9",
                new BigDecimal("59999.00"),
                1
        );

        Order saved =
                orderRepository.saveAndFlush(
                        order
                );

        assertThat(saved.getId())
                .isNotNull();

        assertThat(saved.getStatus())
                .isEqualTo(
                        OrderStatus.CREATED
                );

        assertThat(saved.getItems())
                .hasSize(2);

        assertThat(saved.getTotalAmount())
                .isEqualByComparingTo(
                        "199997.00"
                );
    }

    @Test
    @Transactional
    void shouldFindOrderOnlyForOwner() {

        Order order =
                new Order(CUSTOMER_1);

        order.addItem(
                IPHONE_ID,
                "IPHONE-15",
                "iPhone 15",
                new BigDecimal("69999.00"),
                1
        );

        Order saved =
                orderRepository.saveAndFlush(
                        order
                );

        Optional<Order> ownerResult =
                orderRepository
                        .findByIdAndCustomerId(
                                saved.getId(),
                                CUSTOMER_1
                        );

        Optional<Order> otherCustomerResult =
                orderRepository
                        .findByIdAndCustomerId(
                                saved.getId(),
                                CUSTOMER_2
                        );

        assertThat(ownerResult)
                .isPresent();

        assertThat(otherCustomerResult)
                .isEmpty();
    }
}