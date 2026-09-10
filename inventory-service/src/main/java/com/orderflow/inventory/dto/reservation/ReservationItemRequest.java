package com.orderflow.inventory.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationItemRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Min(
                value = 1,
                message = "Quantity must be greater than zero"
        )
        Integer quantity
) {
}