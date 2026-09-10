package com.orderflow.inventory.dto.reservation;

import com.orderflow.inventory.domain.InventoryReservationItem;

import java.util.UUID;

public record ReservationItemResponse(
        UUID productId,
        int quantity
) {

    public static ReservationItemResponse from(
            InventoryReservationItem item
    ) {

        return new ReservationItemResponse(
                item.getProductId(),
                item.getQuantity()
        );
    }
}