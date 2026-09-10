package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.reservation.ReservationResponse;
import com.orderflow.inventory.dto.reservation.ReserveInventoryRequest;

import com.orderflow.inventory.service.InventoryReservationService;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/internal/inventory/reservations"
)
public class InternalInventoryReservationController {

    private final InventoryReservationService
            reservationService;

    public InternalInventoryReservationController(
            InventoryReservationService reservationService
    ) {

        this.reservationService =
                reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse>
    reserve(

            @Valid
            @RequestBody
            ReserveInventoryRequest request
    ) {

        ReservationResponse response =
                reservationService.reserve(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ReservationResponse>
    confirm(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                reservationService.confirm(
                        orderId
                )
        );
    }

    @PostMapping("/{orderId}/release")
    public ResponseEntity<ReservationResponse>
    release(

            @PathVariable
            UUID orderId
    ) {

        return ResponseEntity.ok(
                reservationService.release(
                        orderId
                )
        );
    }
}