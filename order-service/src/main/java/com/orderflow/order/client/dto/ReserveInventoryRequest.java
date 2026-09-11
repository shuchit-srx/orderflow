package com.orderflow.order.client.dto;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        UUID orderId,
        List<ReserveInventoryItemRequest> items
) {
}