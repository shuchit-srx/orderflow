package com.orderflow.inventory.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank(message = "Name is required")
        @Size(
                max = 200,
                message = "Name cannot exceed 200 characters"
        )
        String name,

        @Size(
                max = 2000,
                message = "Description cannot exceed 2000 characters"
        )
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.00",
                message = "Price cannot be negative"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Price must have at most 2 decimal places"
        )
        BigDecimal price
) {

    public UpdateProductRequest {

        if (name != null) {
            name = name.trim();
        }

        if (description != null) {

            description = description.trim();

            if (description.isBlank()) {
                description = null;
            }
        }
    }
}