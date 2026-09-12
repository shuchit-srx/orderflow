package com.orderflow.order.repository;

import com.orderflow.order.domain.Order;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId")
            UUID orderId
    );

    @EntityGraph(
            attributePaths = "items"
    )
    Optional<Order> findByCustomerIdAndIdempotencyKey(
            UUID customerId,
            String idempotencyKey
    );
}