package com.orderflow.order.support;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({
        "SqlWithoutWhere",
        "SqlNoDataSourceInspection",
        "SqlResolve"
})
public abstract class AbstractIntegrationTest {

    protected static final UUID CUSTOMER_1 =
            UUID.fromString(
                    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
            );

    protected static final UUID CUSTOMER_2 =
            UUID.fromString(
                    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
            );

    protected static final UUID IPHONE_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    protected static final UUID PIXEL_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:17-alpine"
                    )
            )
                    .withDatabaseName(
                            "orderflow_order_test"
                    )
                    .withUsername("orderflow")
                    .withPassword("orderflow");

    static {
        POSTGRES.start();
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
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {

        jdbcTemplate.update(
                "DELETE FROM order_items"
        );

        jdbcTemplate.update(
                "DELETE FROM orders"
        );
    }

    protected String orderStatus(
            UUID orderId
    ) {

        String value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM orders
                        WHERE id = ?
                        """,
                        String.class,
                        orderId
                );

        return Objects.requireNonNull(value);
    }

    protected BigDecimal total(
            UUID orderId
    ) {

        BigDecimal value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT total_amount
                        FROM orders
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        orderId
                );

        return Objects.requireNonNull(value);
    }

    protected long orderCount() {

        Long value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        """,
                        Long.class
                );

        return Objects.requireNonNull(value);
    }
}