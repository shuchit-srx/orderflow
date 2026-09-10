package com.orderflow.inventory.scheduler;

import com.orderflow.inventory.domain.ReservationStatus;
import com.orderflow.inventory.repository.InventoryReservationRepository;
import com.orderflow.inventory.service.InventoryReservationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ReservationExpiryScheduler {

    private final InventoryReservationRepository
            reservationRepository;

    private final InventoryReservationService
            reservationService;

    public ReservationExpiryScheduler(
            InventoryReservationRepository reservationRepository,
            InventoryReservationService reservationService
    ) {

        this.reservationRepository =
                reservationRepository;

        this.reservationService =
                reservationService;
    }

    @Scheduled(
            fixedDelayString =
                    "${inventory.reservation.expiry-scan-ms:60000}"
    )
    public void expireReservations() {

        Instant now = Instant.now();

        var candidates =
                reservationRepository
                        .findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                                ReservationStatus.ACTIVE,
                                now
                        );

        for (var reservation : candidates) {

            reservationService.expireIfActive(
                    reservation.getOrderId(),
                    now
            );
        }
    }
}