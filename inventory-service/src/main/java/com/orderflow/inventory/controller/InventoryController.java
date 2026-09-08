package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.inventory.InventoryResponse;
import com.orderflow.inventory.service.InventoryService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService =
                inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable UUID productId
    ) {

        InventoryResponse response =
                inventoryService.getInventory(
                        productId
                );

        return ResponseEntity.ok(response);
    }
}
