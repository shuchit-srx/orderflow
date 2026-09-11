package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.InventoryReservation;
import com.orderflow.inventory.dto.reservation.ReservationItemRequest;
import com.orderflow.inventory.dto.reservation.ReservationResponse;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.ProductNotFoundException;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.InventoryReservationRepository;
import com.orderflow.inventory.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryReservationCreationService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;
    private final long reservationTtlSeconds;

    public InventoryReservationCreationService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            InventoryReservationRepository reservationRepository,
            @Value("${inventory.reservation.ttl-seconds:900}")
            long reservationTtlSeconds
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.reservationTtlSeconds = reservationTtlSeconds;
    }

    @Transactional
    public ReservationResponse create(
            UUID orderId,
            String requestHash,
            List<ReservationItemRequest> items
    ) {

        InventoryReservation reservation =
                new InventoryReservation(
                        orderId,
                        requestHash,
                        Instant.now()
                                .plusSeconds(reservationTtlSeconds)
                );

        reservationRepository.saveAndFlush(reservation);

        for (ReservationItemRequest item : items) {

            UUID productId = item.productId();
            int quantity = item.quantity();

            if (!productRepository
                    .existsByIdAndActiveTrue(productId)) {

                throw new ProductNotFoundException(
                        productId
                );
            }

            int updatedRows =
                    inventoryRepository.reserveStock(
                            productId,
                            quantity
                    );

            if (updatedRows == 0) {

                if (!inventoryRepository.existsById(productId)) {

                    throw new InventoryNotFoundException(
                            productId
                    );
                }

                throw new InsufficientStockException(
                        productId,
                        quantity
                );
            }
        }

        for (ReservationItemRequest item : items) {

            reservation.addItem(
                    item.productId(),
                    item.quantity()
            );
        }

        InventoryReservation saved =
                reservationRepository.saveAndFlush(
                        reservation
                );

        return ReservationResponse.from(saved);
    }
}