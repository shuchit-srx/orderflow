package com.orderflow.order.controller;

import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderPageResponse;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.security.CurrentCustomer;
import com.orderflow.order.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentCustomer currentCustomer;

    public OrderController(
            OrderService orderService,
            CurrentCustomer currentCustomer
    ) {

        this.orderService = orderService;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(

            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {

        UUID customerId =
                currentCustomer.id(jwt);

        OrderResponse response =
                orderService.createOrder(
                        customerId,
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

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID orderId
    ) {

        UUID customerId =
                currentCustomer.id(jwt);

        return ResponseEntity.ok(
                orderService.getOrder(
                        customerId,
                        orderId
                )
        );
    }

    @GetMapping
    public ResponseEntity<OrderPageResponse> getOrders(

            @AuthenticationPrincipal
            Jwt jwt,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        UUID customerId =
                currentCustomer.id(jwt);

        return ResponseEntity.ok(
                orderService.getOrders(
                        customerId,
                        page,
                        size
                )
        );
    }
}