package com.orderflow.inventory.controller;

import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc
class InventoryOversellIntegrationTest
        extends AbstractIntegrationTest {

    private static final UUID PRODUCT_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final String INTERNAL_TOKEN_HEADER =
            "X-Internal-Service-Token";

    private static final String INTERNAL_TOKEN =
            "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void prepareInventory() {

        jdbcTemplate.update(
                """
                DELETE FROM inventory_reservation_items
                WHERE product_id = ?
                """,
                PRODUCT_ID
        );

        /*
         * Remove reservation rows that no longer
         * contain items after cleanup.
         */
        jdbcTemplate.update(
                """
                DELETE FROM inventory_reservations r
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM inventory_reservation_items i
                    WHERE i.reservation_id = r.id
                )
                """
        );

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET available_quantity = 10,
                    reserved_quantity = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE product_id = ?
                """,
                PRODUCT_ID
        );
    }

    @Test
    void concurrentReservationsMustNotOversell()
            throws Exception {

        int concurrentRequests = 25;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        concurrentRequests
                );

        CountDownLatch ready =
                new CountDownLatch(
                        concurrentRequests
                );

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Integer>> futures =
                new ArrayList<>();

        try {

            for (
                    int i = 0;
                    i < concurrentRequests;
                    i++
            ) {

                UUID orderId =
                        UUID.randomUUID();

                futures.add(
                        executor.submit(
                                () -> {

                                    ready.countDown();

                                    start.await();

                                    return reserve(
                                            orderId,
                                            1
                                    );
                                }
                        )
                );
            }

            ready.await();

            start.countDown();

            int created =
                    0;

            int conflicts =
                    0;

            for (
                    Future<Integer> future :
                    futures
            ) {

                int status =
                        future.get();

                if (status == 201) {
                    created++;
                }

                if (status == 409) {
                    conflicts++;
                }
            }

            assertThat(created)
                    .isEqualTo(10);

            assertThat(conflicts)
                    .isEqualTo(15);

            Integer available =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT available_quantity
                            FROM inventory
                            WHERE product_id = ?
                            """,
                            Integer.class,
                            PRODUCT_ID
                    );

            Integer reserved =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT reserved_quantity
                            FROM inventory
                            WHERE product_id = ?
                            """,
                            Integer.class,
                            PRODUCT_ID
                    );

            assertThat(available)
                    .isZero();

            assertThat(reserved)
                    .isEqualTo(10);

            Integer activeReservations =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM inventory_reservations r
                            JOIN inventory_reservation_items i
                              ON i.reservation_id = r.id
                            WHERE i.product_id = ?
                              AND r.status = 'ACTIVE'
                            """,
                            Integer.class,
                            PRODUCT_ID
                    );

            assertThat(activeReservations)
                    .isEqualTo(10);

        } finally {

            executor.shutdownNow();
        }
    }

    @Test
    void sameOrderIdMustNotReserveStockMoreThanOnce()
            throws Exception {

        UUID orderId =
                UUID.randomUUID();

        int concurrentRequests =
                10;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        concurrentRequests
                );

        CountDownLatch ready =
                new CountDownLatch(
                        concurrentRequests
                );

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Integer>> futures =
                new ArrayList<>();

        try {

            for (
                    int i = 0;
                    i < concurrentRequests;
                    i++
            ) {

                futures.add(
                        executor.submit(
                                () -> {

                                    ready.countDown();

                                    start.await();

                                    return reserve(
                                            orderId,
                                            2
                                    );
                                }
                        )
                );
            }

            ready.await();

            start.countDown();

            for (
                    Future<Integer> future :
                    futures
            ) {

                int status =
                        future.get();

                /*
                 * Depending on how the current
                 * idempotent replay is represented,
                 * concurrent duplicate requests may
                 * return success or a conflict.
                 *
                 * The important invariant is below:
                 * stock can only move once.
                 */
                assertThat(status)
                        .isIn(
                                200,
                                201,
                                409
                        );
            }

            Integer available =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT available_quantity
                            FROM inventory
                            WHERE product_id = ?
                            """,
                            Integer.class,
                            PRODUCT_ID
                    );

            Integer reserved =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT reserved_quantity
                            FROM inventory
                            WHERE product_id = ?
                            """,
                            Integer.class,
                            PRODUCT_ID
                    );

            assertThat(available)
                    .isEqualTo(8);

            assertThat(reserved)
                    .isEqualTo(2);

            Integer reservationCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM inventory_reservations
                            WHERE order_id = ?
                            """,
                            Integer.class,
                            orderId
                    );

            assertThat(reservationCount)
                    .isEqualTo(1);

        } finally {

            executor.shutdownNow();
        }
    }

    @Test
    void confirmedReservationShouldNotLeaveReservedStock()
            throws Exception {

        UUID orderId =
                UUID.randomUUID();

        int status =
                reserve(
                        orderId,
                        3
                );

        assertThat(status)
                .isEqualTo(201);

        mockMvc.perform(
                        post(
                                "/api/v1/internal/inventory/reservations/{orderId}/confirm",
                                orderId
                        )
                                .header(
                                        INTERNAL_TOKEN_HEADER,
                                        INTERNAL_TOKEN
                                )
                )
                .andReturn();

        Integer available =
                jdbcTemplate.queryForObject(
                        """
                        SELECT available_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        PRODUCT_ID
                );

        Integer reserved =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        PRODUCT_ID
                );

        assertThat(available)
                .isEqualTo(7);

        assertThat(reserved)
                .isZero();
    }

    @Test
    void repeatedReleaseMustRestoreInventoryOnlyOnce()
            throws Exception {

        UUID orderId =
                UUID.randomUUID();

        assertThat(
                reserve(
                        orderId,
                        4
                )
        )
                .isEqualTo(201);

        for (
                int i = 0;
                i < 5;
                i++
        ) {

            mockMvc.perform(
                    post(
                            "/api/v1/internal/inventory/reservations/{orderId}/release",
                            orderId
                    )
                            .header(
                                    INTERNAL_TOKEN_HEADER,
                                    INTERNAL_TOKEN
                            )
            );
        }

        Integer available =
                jdbcTemplate.queryForObject(
                        """
                        SELECT available_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        PRODUCT_ID
                );

        Integer reserved =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        PRODUCT_ID
                );

        assertThat(available)
                .isEqualTo(10);

        assertThat(reserved)
                .isZero();

        String reservationStatus =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM inventory_reservations
                        WHERE order_id = ?
                        """,
                        String.class,
                        orderId
                );

        assertThat(reservationStatus)
                .isEqualTo("RELEASED");
    }

    private int reserve(
            UUID orderId,
            int quantity
    ) throws Exception {

        String body =
                """
                {
                  "orderId": "%s",
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": %d
                    }
                  ]
                }
                """
                        .formatted(
                                orderId,
                                PRODUCT_ID,
                                quantity
                        );

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/internal/inventory/reservations"
                                )
                                        .header(
                                                INTERNAL_TOKEN_HEADER,
                                                INTERNAL_TOKEN
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(body)
                        )
                        .andReturn();

        return result
                .getResponse()
                .getStatus();
    }
}