package com.orderflow.user.repository;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("unused")
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = new User(
                "repository@example.com",
                "hashed-password",
                "Repository User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        User saved =
                userRepository.saveAndFlush(user);

        assertThat(saved.getId())
                .isNotNull();

        assertThat(
                userRepository.findByEmail(
                        "repository@example.com"
                )
        )
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo(
                        "repository@example.com"
                );
    }

    @Test
    void shouldReportExistingEmail() {

        User user = new User(
                "existing@example.com",
                "hash",
                "Existing User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        userRepository.saveAndFlush(user);

        assertThat(
                userRepository.existsByEmail(
                        "existing@example.com"
                )
        ).isTrue();

        assertThat(
                userRepository.existsByEmail(
                        "missing@example.com"
                )
        ).isFalse();
    }

    @Test
    void databaseShouldRejectDuplicateEmail() {

        User first = new User(
                "duplicate@example.com",
                "hash-one",
                "First User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        userRepository.saveAndFlush(first);

        User second = new User(
                "duplicate@example.com",
                "hash-two",
                "Second User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(second)
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
}