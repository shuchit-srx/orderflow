package com.orderflow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty(
                message =
                        "At least one order item is required"
        )
        @Size(
                max = 50,
                message =
                        "An order cannot contain more than 50 items"
        )
        List<@Valid CreateOrderItemRequest> items
) {
}