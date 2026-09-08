package com.orderflow.inventory.dto.inventory;

import com.orderflow.inventory.domain.Inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        int availableQuantity,
        boolean inStock,
        Instant updatedAt
) {

    public static InventoryResponse from(
            Inventory inventory
    ) {

        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getAvailableQuantity() > 0,
                inventory.getUpdatedAt()
        );
    }
}