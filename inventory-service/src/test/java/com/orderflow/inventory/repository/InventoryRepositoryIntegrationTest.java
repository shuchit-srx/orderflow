package com.orderflow.inventory.repository;

import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryRepositoryIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @Transactional
    void shouldAtomicallyReserveStock() {

        int updatedRows =
                inventoryRepository.reserveStock(
                        IPHONE_ID,
                        5
                );

        assertThat(updatedRows).isEqualTo(1);

        assertThat(available(IPHONE_ID))
                .isEqualTo(15);

        assertThat(reserved(IPHONE_ID))
                .isEqualTo(5);
    }

    @Test
    @Transactional
    void shouldRejectReservationWhenStockIsInsufficient() {

        int updatedRows =
                inventoryRepository.reserveStock(
                        IPHONE_ID,
                        25
                );

        assertThat(updatedRows).isZero();

        assertThat(available(IPHONE_ID))
                .isEqualTo(20);

        assertThat(reserved(IPHONE_ID))
                .isZero();
    }

    @Test
    @Transactional
    void shouldConfirmReservedStock() {

        inventoryRepository.reserveStock(
                IPHONE_ID,
                5
        );

        int updatedRows =
                inventoryRepository
                        .confirmReservedStock(
                                IPHONE_ID,
                                5
                        );

        assertThat(updatedRows).isEqualTo(1);

        assertThat(available(IPHONE_ID))
                .isEqualTo(15);

        assertThat(reserved(IPHONE_ID))
                .isZero();
    }

    @Test
    @Transactional
    void shouldReleaseReservedStock() {

        inventoryRepository.reserveStock(
                IPHONE_ID,
                5
        );

        int updatedRows =
                inventoryRepository
                        .releaseReservedStock(
                                IPHONE_ID,
                                5
                        );

        assertThat(updatedRows).isEqualTo(1);

        assertThat(available(IPHONE_ID))
                .isEqualTo(20);

        assertThat(reserved(IPHONE_ID))
                .isZero();
    }
}