package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.product.ProductPageResponse;
import com.orderflow.inventory.dto.product.ProductResponse;
import com.orderflow.inventory.service.ProductService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ProductPageResponse> getProducts(

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size,

            @RequestParam(
                    defaultValue = "name"
            )
            String sortBy,

            @RequestParam(
                    defaultValue = "asc"
            )
            String sortDirection,

            @RequestParam(
                    required = false
            )
            String name,

            @RequestParam(
                    required = false
            )
            BigDecimal minPrice,

            @RequestParam(
                    required = false
            )
            BigDecimal maxPrice
    ) {

        ProductPageResponse response =
                productService.getProducts(
                        page,
                        size,
                        sortBy,
                        sortDirection,
                        name,
                        minPrice,
                        maxPrice
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID productId
    ) {

        ProductResponse response =
                productService.getProduct(
                        productId
                );

        return ResponseEntity.ok(response);
    }
}