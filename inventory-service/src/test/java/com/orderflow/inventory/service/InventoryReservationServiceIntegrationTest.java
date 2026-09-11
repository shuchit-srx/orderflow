package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.reservation.ReservationItemRequest;
import com.orderflow.inventory.dto.reservation.ReserveInventoryRequest;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.ReservationConflictException;
import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class InventoryReservationServiceIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private InventoryReservationService reservationService;

    @Test
    void shouldReserveMultipleProducts() {

        UUID orderId = UUID.randomUUID();

        var request =
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        2
                                ),
                                new ReservationItemRequest(
                                        PIXEL_ID,
                                        3
                                )
                        )
                );

        var response =
                reservationService.reserve(request);

        assertThat(response.status().name())
                .isEqualTo("ACTIVE");

        assertThat(available(IPHONE_ID))
                .isEqualTo(18);

        assertThat(reserved(IPHONE_ID))
                .isEqualTo(2);

        assertThat(available(PIXEL_ID))
                .isEqualTo(7);

        assertThat(reserved(PIXEL_ID))
                .isEqualTo(3);

        assertThat(reservationCount(orderId))
                .isEqualTo(1);
    }

    @Test
    void shouldBeIdempotentForSameOrderAndPayload() {

        UUID orderId = UUID.randomUUID();

        var request =
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        5
                                )
                        )
                );

        var first =
                reservationService.reserve(request);

        var second =
                reservationService.reserve(request);

        assertThat(second.reservationId())
                .isEqualTo(first.reservationId());

        assertThat(reservationCount(orderId))
                .isEqualTo(1);

        assertThat(available(IPHONE_ID))
                .isEqualTo(15);

        assertThat(reserved(IPHONE_ID))
                .isEqualTo(5);
    }

    @Test
    void shouldRejectDifferentPayloadForSameOrder() {

        UUID orderId = UUID.randomUUID();

        reservationService.reserve(
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        2
                                )
                        )
                )
        );

        assertThatThrownBy(() ->
                reservationService.reserve(
                        new ReserveInventoryRequest(
                                orderId,
                                List.of(
                                        new ReservationItemRequest(
                                                IPHONE_ID,
                                                4
                                        )
                                )
                        )
                )
        )
                .isInstanceOf(
                        ReservationConflictException.class
                );

        assertThat(available(IPHONE_ID))
                .isEqualTo(18);

        assertThat(reserved(IPHONE_ID))
                .isEqualTo(2);
    }

    @Test
    void shouldRollbackWholeReservationWhenOneItemFails() {

        UUID orderId = UUID.randomUUID();

        var request =
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        5
                                ),
                                new ReservationItemRequest(
                                        PIXEL_ID,
                                        500
                                )
                        )
                );

        assertThatThrownBy(() ->
                reservationService.reserve(request)
        )
                .isInstanceOf(
                        InsufficientStockException.class
                );

        assertThat(available(IPHONE_ID))
                .isEqualTo(20);

        assertThat(reserved(IPHONE_ID))
                .isZero();

        assertThat(available(PIXEL_ID))
                .isEqualTo(10);

        assertThat(reserved(PIXEL_ID))
                .isZero();

        assertThat(reservationCount(orderId))
                .isZero();
    }

    @Test
    void shouldConfirmReservation() {

        UUID orderId = UUID.randomUUID();

        reservationService.reserve(
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        5
                                )
                        )
                )
        );

        reservationService.confirm(orderId);

        assertThat(reservationStatus(orderId))
                .isEqualTo("CONFIRMED");

        assertThat(available(IPHONE_ID))
                .isEqualTo(15);

        assertThat(reserved(IPHONE_ID))
                .isZero();
    }

    @Test
    void shouldReleaseReservation() {

        UUID orderId = UUID.randomUUID();

        reservationService.reserve(
                new ReserveInventoryRequest(
                        orderId,
                        List.of(
                                new ReservationItemRequest(
                                        IPHONE_ID,
                                        5
                                )
                        )
                )
        );

        reservationService.release(orderId);

        assertThat(reservationStatus(orderId))
                .isEqualTo("RELEASED");

        assertThat(available(IPHONE_ID))
                .isEqualTo(20);

        assertThat(reserved(IPHONE_ID))
                .isZero();
    }
}