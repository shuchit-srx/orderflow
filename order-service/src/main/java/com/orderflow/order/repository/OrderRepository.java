package com.orderflow.order.repository;

import com.orderflow.order.domain.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository
        extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndCustomerId(
            UUID id,
            UUID customerId
    );

    Page<Order> findByCustomerId(
            UUID customerId,
            Pageable pageable
    );
}