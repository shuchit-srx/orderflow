package com.orderflow.order.client.dto;

import java.util.UUID;

public record ReserveInventoryItemRequest(
        UUID productId,
        int quantity
) {
}