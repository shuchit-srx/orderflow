package com.orderflow.inventory.dto.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(

        @NotNull(message = "Delta is required")
        @Min(
                value = -1_000_000,
                message = "Delta is too small"
        )
        @Max(
                value = 1_000_000,
                message = "Delta is too large"
        )
        Integer delta
) {
}