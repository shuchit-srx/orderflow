package com.orderflow.order.controller;

import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.security.CurrentCustomer;
import com.orderflow.order.service.OrderSagaService;
import com.orderflow.order.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private final OrderService orderService;

    private final OrderSagaService orderSagaService;

    private final CurrentCustomer currentCustomer;

    public OrderController(
            OrderService orderService,
            OrderSagaService orderSagaService,
            CurrentCustomer currentCustomer
    ) {

        this.orderService =
                orderService;

        this.orderSagaService =
                orderSagaService;

        this.currentCustomer =
                currentCustomer;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal
            Jwt jwt,

            @RequestHeader(
                    name = IDEMPOTENCY_KEY_HEADER,
                    required = false
            )
            String idempotencyKey,

            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {

        UUID customerId =
                currentCustomer.id(jwt);

        OrderResponse response =
                orderSagaService.placeOrder(
                        customerId,
                        idempotencyKey,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
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

        OrderResponse response =
                orderService.getOrder(
                        customerId,
                        orderId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @AuthenticationPrincipal
            Jwt jwt,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        UUID customerId =
                currentCustomer.id(jwt);

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.clamp(
                        size,
                        1,
                        100
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<OrderResponse> response =
                orderService.getOrders(
                        customerId,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}