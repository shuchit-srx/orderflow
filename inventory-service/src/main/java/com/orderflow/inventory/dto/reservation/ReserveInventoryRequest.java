package com.orderflow.inventory.dto.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(

        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotEmpty(message = "At least one item is required")
        @Size(
                max = 50,
                message = "A reservation cannot contain more than 50 products"
        )
        List<@Valid ReservationItemRequest> items
) {
}