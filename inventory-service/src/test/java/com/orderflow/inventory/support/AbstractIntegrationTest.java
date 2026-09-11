package com.orderflow.inventory.support;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({
        "SqlWithoutWhere",
        "SqlNoDataSourceInspection",
        "SqlResolve"
})
public abstract class AbstractIntegrationTest {

    protected static final UUID IPHONE_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    protected static final UUID PIXEL_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    protected static final UUID OLD_PHONE_ID =
            UUID.fromString(
                    "55555555-5555-5555-5555-555555555555"
            );

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:17-alpine"
                    )
            )
                    .withDatabaseName(
                            "orderflow_inventory_test"
                    )
                    .withUsername("orderflow")
                    .withPassword("orderflow");

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "redis:7-alpine"
                    )
            )
                    .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(6379)
        );
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetTestState() {

        clearRedis();

        jdbcTemplate.update(
                "DELETE FROM inventory_reservation_items"
        );

        jdbcTemplate.update(
                "DELETE FROM inventory_reservations"
        );

        jdbcTemplate.update(
                "DELETE FROM inventory"
        );

        jdbcTemplate.update(
                "DELETE FROM products"
        );

        insertProduct(
                IPHONE_ID,
                "IPHONE-15",
                "iPhone 15",
                new BigDecimal("69999.00"),
                true,
                20
        );

        insertProduct(
                PIXEL_ID,
                "PIXEL-9",
                "Google Pixel 9",
                new BigDecimal("59999.00"),
                true,
                10
        );

        insertProduct(
                OLD_PHONE_ID,
                "OLD-PHONE",
                "Old Phone",
                new BigDecimal("5000.00"),
                false,
                5
        );
    }

    protected void insertProduct(
            UUID id,
            String sku,
            String name,
            BigDecimal price,
            boolean active,
            int availableQuantity
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO products (
                    id,
                    sku,
                    name,
                    description,
                    price,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                """,
                id,
                sku,
                name,
                "Test product",
                price,
                active
        );

        jdbcTemplate.update(
                """
                INSERT INTO inventory (
                    product_id,
                    available_quantity,
                    reserved_quantity,
                    updated_at
                )
                VALUES (?, ?, 0, NOW())
                """,
                id,
                availableQuantity
        );
    }

    protected int available(
            UUID productId
    ) {

        Integer value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT available_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        productId
                );

        return Objects.requireNonNull(value);
    }

    protected int reserved(
            UUID productId
    ) {

        Integer value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved_quantity
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Integer.class,
                        productId
                );

        return Objects.requireNonNull(value);
    }

    protected long reservationCount(
            UUID orderId
    ) {

        Long value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM inventory_reservations
                        WHERE order_id = ?
                        """,
                        Long.class,
                        orderId
                );

        return Objects.requireNonNull(value);
    }

    protected String reservationStatus(
            UUID orderId
    ) {

        return jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM inventory_reservations
                WHERE order_id = ?
                """,
                String.class,
                orderId
        );
    }

    private void clearRedis() {

        Set<String> keys =
                redisTemplate.keys("*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}