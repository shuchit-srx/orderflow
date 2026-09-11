package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.InventoryReservation;
import com.orderflow.inventory.domain.ReservationStatus;
import com.orderflow.inventory.dto.reservation.ReservationItemRequest;
import com.orderflow.inventory.dto.reservation.ReservationResponse;
import com.orderflow.inventory.dto.reservation.ReserveInventoryRequest;
import com.orderflow.inventory.exception.InventoryConsistencyException;
import com.orderflow.inventory.exception.InvalidReservationStateException;
import com.orderflow.inventory.exception.ReservationConflictException;
import com.orderflow.inventory.exception.ReservationNotFoundException;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.InventoryReservationRepository;

import org.springframework.dao.DataIntegrityViolationException;
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
    private final InventoryReservationRepository reservationRepository;

    private final InventoryReservationCreationService
            creationService;

    public InventoryReservationService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryReservationCreationService creationService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.creationService = creationService;
    }

    public ReservationResponse reserve(
            ReserveInventoryRequest request
    ) {

        validateUniqueProducts(request.items());

        List<ReservationItemRequest> sortedItems =
                request.items()
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

        var existing =
                reservationRepository.findByOrderId(
                        request.orderId()
                );

        if (existing.isPresent()) {

            return resolveExistingReservation(
                    existing.get(),
                    requestHash
            );
        }

        try {

            return creationService.create(
                    request.orderId(),
                    requestHash,
                    sortedItems
            );

        } catch (DataIntegrityViolationException exception) {

            var winner =
                    reservationRepository.findByOrderId(
                            request.orderId()
                    );

            if (winner.isEmpty()) {
                throw exception;
            }

            return resolveExistingReservation(
                    winner.get(),
                    requestHash
            );
        }
    }

    @Transactional
    public ReservationResponse confirm(
            UUID orderId
    ) {

        InventoryReservation reservation =
                reservationRepository
                        .findByOrderIdForUpdate(orderId)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        orderId
                                )
                        );

        if (reservation.getStatus()
                == ReservationStatus.CONFIRMED) {

            return ReservationResponse.from(
                    reservation
            );
        }

        if (reservation.getStatus()
                != ReservationStatus.ACTIVE) {

            throw new InvalidReservationStateException(
                    orderId,
                    reservation.getStatus(),
                    "confirm"
            );
        }

        for (var item : reservation.getItems()) {

            int updatedRows =
                    inventoryRepository
                            .confirmReservedStock(
                                    item.getProductId(),
                                    item.getQuantity()
                            );

            if (updatedRows != 1) {

                throw new InventoryConsistencyException(
                        item.getProductId()
                );
            }
        }

        reservation.markConfirmed();

        InventoryReservation saved =
                reservationRepository.saveAndFlush(
                        reservation
                );

        return ReservationResponse.from(saved);
    }

    @Transactional
    public ReservationResponse release(
            UUID orderId
    ) {

        InventoryReservation reservation =
                reservationRepository
                        .findByOrderIdForUpdate(orderId)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        orderId
                                )
                        );

        if (reservation.getStatus()
                == ReservationStatus.RELEASED) {

            return ReservationResponse.from(
                    reservation
            );
        }

        if (reservation.getStatus()
                == ReservationStatus.EXPIRED) {

            return ReservationResponse.from(
                    reservation
            );
        }

        if (reservation.getStatus()
                == ReservationStatus.CONFIRMED) {

            throw new InvalidReservationStateException(
                    orderId,
                    reservation.getStatus(),
                    "release"
            );
        }

        restoreReservedStock(reservation);

        reservation.markReleased();

        InventoryReservation saved =
                reservationRepository.saveAndFlush(
                        reservation
                );

        return ReservationResponse.from(saved);
    }

    @Transactional
    public boolean expireIfActive(
            UUID orderId,
            Instant now
    ) {

        InventoryReservation reservation =
                reservationRepository
                        .findByOrderIdForUpdate(orderId)
                        .orElse(null);

        if (reservation == null) {
            return false;
        }

        if (reservation.getStatus()
                != ReservationStatus.ACTIVE) {

            return false;
        }

        if (reservation.getExpiresAt()
                .isAfter(now)) {

            return false;
        }

        restoreReservedStock(reservation);

        reservation.markExpired();

        reservationRepository.saveAndFlush(
                reservation
        );

        return true;
    }

    private ReservationResponse resolveExistingReservation(
            InventoryReservation reservation,
            String requestHash
    ) {

        if (!reservation
                .getRequestHash()
                .equals(requestHash)) {

            throw new ReservationConflictException(
                    reservation.getOrderId()
            );
        }

        return ReservationResponse.from(
                reservation
        );
    }

    private void restoreReservedStock(
            InventoryReservation reservation
    ) {

        for (var item : reservation.getItems()) {

            int updatedRows =
                    inventoryRepository
                            .releaseReservedStock(
                                    item.getProductId(),
                                    item.getQuantity()
                            );

            if (updatedRows != 1) {

                throw new InventoryConsistencyException(
                        item.getProductId()
                );
            }
        }
    }

    private void validateUniqueProducts(
            List<ReservationItemRequest> items
    ) {

        Set<UUID> productIds = new HashSet<>();

        for (ReservationItemRequest item : items) {

            if (!productIds.add(item.productId())) {

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

        for (ReservationItemRequest item : items) {

            canonical
                    .append('|')
                    .append(item.productId())
                    .append(':')
                    .append(item.quantity());
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