package com.orderflow.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;

    @Column(
            name = "product_id",
            nullable = false
    )
    private UUID productId;

    @Column(
            nullable = false,
            length = 100
    )
    private String sku;

    @Column(
            name = "product_name",
            nullable = false,
            length = 200
    )
    private String productName;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            nullable = false
    )
    private int quantity;

    @Column(
            name = "line_total",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    OrderItem(
            Order order,
            UUID productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Order item quantity must be greater than zero"
            );
        }

        if (unitPrice == null
                || unitPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        this.order = order;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;

        this.lineTotal =
                unitPrice.multiply(
                        BigDecimal.valueOf(quantity)
                );
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}