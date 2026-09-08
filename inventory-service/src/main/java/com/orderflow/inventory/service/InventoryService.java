package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.inventory.InventoryResponse;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.exception.ProductNotFoundException;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository
    ) {
        this.inventoryRepository =
                inventoryRepository;

        this.productRepository =
                productRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(
            UUID productId
    ) {

        /*
         * Customers should not retrieve inventory
         * for inactive/deleted products.
         */
        if (productRepository
                .findByIdAndActiveTrue(productId)
                .isEmpty()) {

            throw new ProductNotFoundException(
                    productId
            );
        }

        Inventory inventory =
                inventoryRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new InventoryNotFoundException(
                                        productId
                                )
                        );

        return InventoryResponse.from(
                inventory
        );
    }
}