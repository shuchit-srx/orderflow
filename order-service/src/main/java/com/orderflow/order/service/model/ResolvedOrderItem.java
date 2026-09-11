package com.orderflow.order.service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ResolvedOrderItem(
        UUID productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}