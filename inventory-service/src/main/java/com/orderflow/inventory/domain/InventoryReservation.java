package com.orderflow.inventory.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_inventory_reservations_order_id",
                        columnNames = "order_id"
                )
        }
)
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "order_id",
            nullable = false,
            unique = true
    )
    private UUID orderId;

    @Column(
            name = "request_hash",
            nullable = false,
            length = 64
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ReservationStatus status;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @OneToMany(
            mappedBy = "reservation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InventoryReservationItem> items =
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

    protected InventoryReservation() {
    }

    public InventoryReservation(
            UUID orderId,
            String requestHash,
            Instant expiresAt
    ) {

        this.orderId = orderId;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<InventoryReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void addItem(
            UUID productId,
            int quantity
    ) {

        InventoryReservationItem item =
                new InventoryReservationItem(
                        this,
                        productId,
                        quantity
                );

        items.add(item);
    }

    public void markConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public void markExpired() {
        this.status = ReservationStatus.EXPIRED;
    }
}