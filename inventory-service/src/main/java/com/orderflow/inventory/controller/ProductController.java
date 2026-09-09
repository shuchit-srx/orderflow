package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.product.CreateProductRequest;
import com.orderflow.inventory.dto.product.ProductPageResponse;
import com.orderflow.inventory.dto.product.ProductResponse;
import com.orderflow.inventory.dto.product.UpdateProductRequest;

import com.orderflow.inventory.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService =
                productService;
    }

    @GetMapping
    public ResponseEntity<ProductPageResponse> getProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(defaultValue = "name")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String sortDirection,

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            BigDecimal minPrice,

            @RequestParam(required = false)
            BigDecimal maxPrice
    ) {

        return ResponseEntity.ok(
                productService.getProducts(
                        page,
                        size,
                        sortBy,
                        sortDirection,
                        name,
                        minPrice,
                        maxPrice
                )
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID productId
    ) {

        return ResponseEntity.ok(
                productService.getProduct(
                        productId
                )
        );
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid
            @RequestBody
            CreateProductRequest request
    ) {

        ProductResponse product =
                productService.createProduct(
                        request
                );

        URI location =
                URI.create(
                        "/api/v1/products/"
                                + product.id()
                );

        return ResponseEntity
                .created(location)
                .body(product);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID productId,

            @Valid
            @RequestBody
            UpdateProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(
                        productId,
                        request
                )
        );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable UUID productId
    ) {

        productService.deactivateProduct(
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}