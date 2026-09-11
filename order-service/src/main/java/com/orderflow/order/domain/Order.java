package com.orderflow.order.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.orderflow.order.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "customer_id",
            nullable = false
    )
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private OrderStatus status;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalAmount;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items =
            new ArrayList<>();

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Order() {
    }

    public Order(UUID customerId) {
        this.customerId = customerId;
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(
            UUID productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {

        OrderItem item =
                new OrderItem(
                        this,
                        productId,
                        sku,
                        productName,
                        unitPrice,
                        quantity
                );

        items.add(item);

        recalculateTotal();
    }

    private void recalculateTotal() {

        this.totalAmount =
                items.stream()
                        .map(OrderItem::getLineTotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );
    }

    public void markInventoryReserved() {

        if (status == OrderStatus.INVENTORY_RESERVED) {
            return;
        }

        requireState(
                OrderStatus.CREATED,
                OrderStatus.INVENTORY_RESERVED
        );

        this.status =
                OrderStatus.INVENTORY_RESERVED;
    }

    public void confirm() {

        if (status == OrderStatus.CONFIRMED) {
            return;
        }

        requireState(
                OrderStatus.INVENTORY_RESERVED,
                OrderStatus.CONFIRMED
        );

        this.status =
                OrderStatus.CONFIRMED;
    }

    public void cancel() {

        if (status == OrderStatus.CANCELLED) {
            return;
        }

        if (status != OrderStatus.CREATED
                && status != OrderStatus.INVENTORY_RESERVED) {

            throw new InvalidOrderStateException(
                    id,
                    status,
                    OrderStatus.CANCELLED
            );
        }

        this.status =
                OrderStatus.CANCELLED;
    }

    public void fail() {

        if (status == OrderStatus.FAILED) {
            return;
        }

        if (status != OrderStatus.CREATED
                && status != OrderStatus.INVENTORY_RESERVED) {

            throw new InvalidOrderStateException(
                    id,
                    status,
                    OrderStatus.FAILED
            );
        }

        this.status =
                OrderStatus.FAILED;
    }

    private void requireState(
            OrderStatus requiredStatus,
            OrderStatus targetStatus
    ) {

        if (status != requiredStatus) {

            throw new InvalidOrderStateException(
                    id,
                    status,
                    targetStatus
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}