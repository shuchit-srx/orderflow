package com.orderflow.notification.support;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            )
                    .withDatabaseName(
                            "orderflow_notification_test"
                    )
                    .withUsername(
                            "orderflow"
                    )
                    .withPassword(
                            "orderflow"
                    );

    static {

        postgres.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @BeforeEach
    void cleanDatabase() {

        jdbcTemplate.update(
                "DELETE FROM processed_events"
        );
    }
}