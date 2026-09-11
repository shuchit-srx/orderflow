package com.orderflow.order.client;

import com.orderflow.order.client.dto.InventoryProductResponse;

import java.util.UUID;

public interface InventoryClient {

    InventoryProductResponse getProduct(
            UUID productId
    );
}