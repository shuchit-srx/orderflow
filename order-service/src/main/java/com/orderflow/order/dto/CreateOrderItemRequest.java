package com.orderflow.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(

        @NotNull
        UUID productId,

        @NotBlank
        @Size(max = 100)
        String sku,

        @NotBlank
        @Size(max = 200)
        String productName,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal unitPrice,

        @NotNull
        @Min(1)
        Integer quantity
) {
}