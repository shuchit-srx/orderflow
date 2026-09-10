package com.orderflow.inventory.dto.inventory;

import com.orderflow.inventory.domain.Inventory;

import java.time.Instant;
import java.util.UUID;

public record AdminInventoryResponse(
        UUID productId,
        int availableQuantity,
        int reservedQuantity,
        Instant updatedAt
) {

    public static AdminInventoryResponse from(
            Inventory inventory
    ) {

        return new AdminInventoryResponse(
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getUpdatedAt()
        );
    }
}