package com.orderflow.inventory.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "inventory_reservation_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reservation_product",
                        columnNames = {
                                "reservation_id",
                                "product_id"
                        }
                )
        }
)
public class InventoryReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reservation_id",
            nullable = false
    )
    private InventoryReservation reservation;

    @Column(
            name = "product_id",
            nullable = false
    )
    private UUID productId;

    @Column(
            nullable = false
    )
    private int quantity;

    protected InventoryReservationItem() {
    }

    InventoryReservationItem(
            InventoryReservation reservation,
            UUID productId,
            int quantity
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        this.reservation = reservation;
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}