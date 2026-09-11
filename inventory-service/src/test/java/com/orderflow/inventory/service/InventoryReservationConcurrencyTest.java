package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.reservation.ReservationItemRequest;
import com.orderflow.inventory.dto.reservation.ReserveInventoryRequest;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.InvalidReservationStateException;
import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationConcurrencyTest
        extends AbstractIntegrationTest {

    @Autowired
    private InventoryReservationService reservationService;

    @Test
    void concurrentIdenticalRequestsShouldReserveOnlyOnce()
            throws Exception {

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

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            List<Future<UUID>> futures =
                    new ArrayList<>();

            for (int i = 0; i < 10; i++) {

                futures.add(
                        executor.submit(() -> {

                            start.await();

                            return reservationService
                                    .reserve(request)
                                    .reservationId();
                        })
                );
            }

            start.countDown();

            var reservationIds =
                    new HashSet<UUID>();

            for (Future<UUID> future : futures) {
                reservationIds.add(
                        future.get(
                                15,
                                TimeUnit.SECONDS
                        )
                );
            }

            assertThat(reservationIds)
                    .hasSize(1);

            assertThat(reservationCount(orderId))
                    .isEqualTo(1);

            assertThat(available(IPHONE_ID))
                    .isEqualTo(15);

            assertThat(reserved(IPHONE_ID))
                    .isEqualTo(5);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentOrdersShouldNotOversell()
            throws Exception {

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET available_quantity = 5,
                    reserved_quantity = 0
                WHERE product_id = ?
                """,
                IPHONE_ID
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            Callable<Boolean> reserve =
                    () -> {

                        start.await();

                        try {

                            reservationService.reserve(
                                    new ReserveInventoryRequest(
                                            UUID.randomUUID(),
                                            List.of(
                                                    new ReservationItemRequest(
                                                            IPHONE_ID,
                                                            4
                                                    )
                                            )
                                    )
                            );

                            return true;

                        } catch (
                                InsufficientStockException exception
                        ) {

                            return false;
                        }
                    };

            Future<Boolean> first =
                    executor.submit(reserve);

            Future<Boolean> second =
                    executor.submit(reserve);

            start.countDown();

            List<Boolean> results =
                    List.of(
                            first.get(
                                    15,
                                    TimeUnit.SECONDS
                            ),
                            second.get(
                                    15,
                                    TimeUnit.SECONDS
                            )
                    );

            assertThat(results)
                    .containsExactlyInAnyOrder(
                            true,
                            false
                    );

            assertThat(available(IPHONE_ID))
                    .isEqualTo(1);

            assertThat(reserved(IPHONE_ID))
                    .isEqualTo(4);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmAndReleaseRaceShouldRemainConsistent()
            throws Exception {

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

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            Callable<Boolean> confirm =
                    () -> {

                        start.await();

                        try {
                            reservationService.confirm(
                                    orderId
                            );

                            return true;

                        } catch (
                                InvalidReservationStateException exception
                        ) {

                            return false;
                        }
                    };

            Callable<Boolean> release =
                    () -> {

                        start.await();

                        try {
                            reservationService.release(
                                    orderId
                            );

                            return true;

                        } catch (
                                InvalidReservationStateException exception
                        ) {

                            return false;
                        }
                    };

            Future<Boolean> confirmResult =
                    executor.submit(confirm);

            Future<Boolean> releaseResult =
                    executor.submit(release);

            start.countDown();

            List<Boolean> results =
                    List.of(
                            confirmResult.get(
                                    15,
                                    TimeUnit.SECONDS
                            ),
                            releaseResult.get(
                                    15,
                                    TimeUnit.SECONDS
                            )
                    );

            assertThat(results)
                    .containsExactlyInAnyOrder(
                            true,
                            false
                    );

            String status =
                    reservationStatus(orderId);

            assertThat(status)
                    .isIn(
                            "CONFIRMED",
                            "RELEASED"
                    );

            assertThat(reserved(IPHONE_ID))
                    .isZero();

            if (status.equals("CONFIRMED")) {

                assertThat(available(IPHONE_ID))
                        .isEqualTo(15);

            } else {

                assertThat(available(IPHONE_ID))
                        .isEqualTo(20);
            }

        } finally {
            executor.shutdownNow();
        }
    }
}