package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.inventory.AdminInventoryResponse;
import com.orderflow.inventory.dto.inventory.StockAdjustmentRequest;
import com.orderflow.inventory.service.InventoryService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService =
                inventoryService;
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<AdminInventoryResponse>
    adjustInventory(

            @PathVariable
            UUID productId,

            @Valid
            @RequestBody
            StockAdjustmentRequest request
    ) {

        return ResponseEntity.ok(
                inventoryService.adjustInventory(
                        productId,
                        request
                )
        );
    }
}