package com.orderflow.order.controller;

import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderService;
import com.orderflow.order.service.OrderFinalizationService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/internal/orders"
)
public class InternalOrderController {

    private final OrderService orderService;
    private final OrderFinalizationService orderFinalizationService;

    public InternalOrderController(
            OrderService orderService,
            OrderFinalizationService orderFinalizationService
    ) {
        this.orderService = orderService;
        this.orderFinalizationService = orderFinalizationService;
    }

    @PostMapping(
            "/{orderId}/inventory-reserved"
    )
    public ResponseEntity<OrderResponse>
    markInventoryReserved(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService
                        .markInventoryReserved(
                                orderId
                        )
        );
    }

    @PostMapping(
            "/{orderId}/confirm"
    )
    public ResponseEntity<OrderResponse>
    confirm(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                orderFinalizationService
                        .confirmOrderAndCreateOutbox(
                                orderId
                        )
        );
    }

    @PostMapping(
            "/{orderId}/cancel"
    )
    public ResponseEntity<OrderResponse>
    cancel(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.cancelOrder(
                        orderId
                )
        );
    }

    @PostMapping(
            "/{orderId}/fail"
    )
    public ResponseEntity<OrderResponse>
    fail(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.failOrder(
                        orderId
                )
        );
    }
}