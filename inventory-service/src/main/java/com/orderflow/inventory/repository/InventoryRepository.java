package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.Inventory;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository
        extends JpaRepository<Inventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.productId = :productId
            """)
    Optional<Inventory> findByProductIdForUpdate(
            @Param("productId")
            UUID productId
    );

    @Modifying
    @Query(
            value = """
                    UPDATE inventory
                    SET available_quantity =
                            available_quantity - :quantity,
                        reserved_quantity =
                            reserved_quantity + :quantity,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE product_id = :productId
                      AND available_quantity >= :quantity
                    """,
            nativeQuery = true
    )
    int reserveStock(
            @Param("productId")
            UUID productId,

            @Param("quantity")
            int quantity
    );

    @Modifying
    @Query(
            value = """
                UPDATE inventory
                SET reserved_quantity =
                        reserved_quantity - :quantity,
                    updated_at = CURRENT_TIMESTAMP
                WHERE product_id = :productId
                  AND reserved_quantity >= :quantity
                """,
            nativeQuery = true
    )
    int confirmReservedStock(
            @Param("productId")
            UUID productId,

            @Param("quantity")
            int quantity
    );

    @Modifying
    @Query(
            value = """
                UPDATE inventory
                SET available_quantity =
                        available_quantity + :quantity,
                    reserved_quantity =
                        reserved_quantity - :quantity,
                    updated_at = CURRENT_TIMESTAMP
                WHERE product_id = :productId
                  AND reserved_quantity >= :quantity
                """,
            nativeQuery = true
    )
    int releaseReservedStock(
            @Param("productId")
            UUID productId,

            @Param("quantity")
            int quantity
    );

    @Modifying
    @Query(
            value = """
                UPDATE inventory
                SET available_quantity =
                        available_quantity + :quantity,
                    updated_at = CURRENT_TIMESTAMP
                WHERE product_id = :productId
                """,
            nativeQuery = true
    )
    int restoreConfirmedStock(
            @Param("productId")
            UUID productId,

            @Param("quantity")
            int quantity
    );
}