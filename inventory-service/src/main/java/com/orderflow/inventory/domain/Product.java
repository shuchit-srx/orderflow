package com.orderflow.inventory.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String sku;

    @Column(
            nullable = false,
            length = 200
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

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

    protected Product() {
    }

    public Product(
            String sku,
            String name,
            String description,
            BigDecimal price
    ) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal price
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public void deactivate() {
        this.active = false;
    }
}