package com.orderflow.order.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
}