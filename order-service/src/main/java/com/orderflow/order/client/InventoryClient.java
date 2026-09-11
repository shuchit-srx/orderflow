package com.orderflow.order.client;

import com.orderflow.order.client.dto.InventoryProductResponse;
import com.orderflow.order.client.dto.ReserveInventoryRequest;

import java.util.UUID;

public interface InventoryClient {

    InventoryProductResponse getProduct(
            UUID productId
    );

    void reserveInventory(
            ReserveInventoryRequest request
    );

    void confirmReservation(
            UUID orderId
    );

    void releaseReservation(
            UUID orderId
    );
}