package com.orderflow.order.controller;

import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderPageResponse;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {

        OrderResponse response =
                orderService.createOrder(
                        request
                );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/orders/"
                                        + response.id()
                        )
                )
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.getOrder(
                        orderId
                )
        );
    }

    @GetMapping
    public ResponseEntity<OrderPageResponse> getOrders(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                orderService.getOrders(
                        page,
                        size
                )
        );
    }
}