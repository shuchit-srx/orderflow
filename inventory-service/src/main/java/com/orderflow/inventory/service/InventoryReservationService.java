package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.InventoryReservation;

import com.orderflow.inventory.dto.reservation.ReservationItemRequest;
import com.orderflow.inventory.dto.reservation.ReservationResponse;
import com.orderflow.inventory.dto.reservation.ReserveInventoryRequest;

import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.ProductNotFoundException;
import com.orderflow.inventory.exception.ReservationConflictException;

import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.InventoryReservationRepository;
import com.orderflow.inventory.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Instant;

import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    private final InventoryReservationRepository
            reservationRepository;

    private final long reservationTtlSeconds;

    public InventoryReservationService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            InventoryReservationRepository reservationRepository,

            @Value(
                    "${inventory.reservation.ttl-seconds:900}"
            )
            long reservationTtlSeconds
    ) {

        this.inventoryRepository =
                inventoryRepository;

        this.productRepository =
                productRepository;

        this.reservationRepository =
                reservationRepository;

        this.reservationTtlSeconds =
                reservationTtlSeconds;
    }

    @Transactional
    public ReservationResponse reserve(
            ReserveInventoryRequest request
    ) {

        validateUniqueProducts(
                request.items()
        );

        /*
         * Consistent order:
         *
         * 1. Gives stable request hashes.
         * 2. Makes DB row lock acquisition deterministic.
         * 3. Reduces deadlock risk for multi-product orders.
         */
        List<ReservationItemRequest> sortedItems =
                request
                        .items()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        item ->
                                                item.productId()
                                                        .toString()
                                )
                        )
                        .toList();

        String requestHash =
                calculateRequestHash(
                        request.orderId(),
                        sortedItems
                );

        /*
         * Sequential retry / service idempotency.
         */
        var existing =
                reservationRepository
                        .findByOrderId(
                                request.orderId()
                        );

        if (existing.isPresent()) {

            InventoryReservation reservation =
                    existing.get();

            if (!reservation
                    .getRequestHash()
                    .equals(requestHash)) {

                throw new ReservationConflictException(
                        request.orderId()
                );
            }

            /*
             * Same order + same payload:
             * do NOT reserve stock again.
             */
            return ReservationResponse.from(
                    reservation
            );
        }

        /*
         * All stock modifications happen inside
         * one database transaction.
         */
        for (ReservationItemRequest item
                : sortedItems) {

            UUID productId =
                    item.productId();

            int quantity =
                    item.quantity();

            /*
             * Inactive/nonexistent products
             * cannot be reserved.
             */
            if (!productRepository
                    .existsByIdAndActiveTrue(
                            productId
                    )) {

                throw new ProductNotFoundException(
                        productId
                );
            }

            /*
             * THIS is the atomic anti-oversell step.
             */
            int updatedRows =
                    inventoryRepository
                            .reserveStock(
                                    productId,
                                    quantity
                            );

            if (updatedRows == 0) {

                /*
                 * Distinguish broken/missing inventory
                 * from normal insufficient-stock failure.
                 */
                if (!inventoryRepository
                        .existsById(productId)) {

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

        Instant expiresAt =
                Instant.now()
                        .plusSeconds(
                                reservationTtlSeconds
                        );

        InventoryReservation reservation =
                new InventoryReservation(
                        request.orderId(),
                        requestHash,
                        expiresAt
                );

        for (ReservationItemRequest item
                : sortedItems) {

            reservation.addItem(
                    item.productId(),
                    item.quantity()
            );
        }

        InventoryReservation saved =
                reservationRepository
                        .saveAndFlush(
                                reservation
                        );

        return ReservationResponse.from(
                saved
        );
    }

    private void validateUniqueProducts(
            List<ReservationItemRequest> items
    ) {

        Set<UUID> productIds =
                new HashSet<>();

        for (ReservationItemRequest item
                : items) {

            if (!productIds.add(
                    item.productId()
            )) {

                throw new IllegalArgumentException(
                        "Duplicate product in reservation: "
                                + item.productId()
                );
            }
        }
    }

    private String calculateRequestHash(
            UUID orderId,
            List<ReservationItemRequest> items
    ) {

        StringBuilder canonical =
                new StringBuilder(
                        orderId.toString()
                );

        for (ReservationItemRequest item
                : items) {

            canonical
                    .append('|')
                    .append(
                            item.productId()
                    )
                    .append(':')
                    .append(
                            item.quantity()
                    );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            canonical
                                    .toString()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}