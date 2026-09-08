package com.orderflow.inventory.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(
            name = "available_quantity",
            nullable = false
    )
    private int availableQuantity;

    @Column(
            name = "reserved_quantity",
            nullable = false
    )
    private int reservedQuantity;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Inventory() {
    }

    public Inventory(UUID productId) {
        this.productId = productId;
        this.availableQuantity = 0;
        this.reservedQuantity = 0;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void adjustAvailableQuantity(int delta) {

        int newQuantity =
                availableQuantity + delta;

        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "Available quantity cannot be negative"
            );
        }

        availableQuantity = newQuantity;
    }
}