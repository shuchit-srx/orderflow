package com.orderflow.user.service;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;
import com.orderflow.user.dto.user.UpdateUserRequest;
import com.orderflow.user.dto.user.UserResponse;
import com.orderflow.user.exception.AccountDisabledException;
import com.orderflow.user.exception.UserNotFoundException;
import com.orderflow.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private UUID userId;

    @BeforeEach
    void setUp() {

        userService =
                new UserService(userRepository);

        userId = UUID.randomUUID();
    }

    @Test
    void getCurrentUserShouldReturnActiveUser() {

        User user = activeUser();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getCurrentUser(userId);

        assertThat(response.email())
                .isEqualTo("test@example.com");

        assertThat(response.displayName())
                .isEqualTo("Test User");

        assertThat(response.role())
                .isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void updateCurrentUserShouldChangeDisplayName() {

        User user = activeUser();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.updateCurrentUser(
                        userId,
                        new UpdateUserRequest(
                                "Updated User"
                        )
                );

        assertThat(response.displayName())
                .isEqualTo("Updated User");

        assertThat(user.getDisplayName())
                .isEqualTo("Updated User");
    }

    @Test
    void getCurrentUserShouldThrowWhenMissing() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getCurrentUser(userId)
        ).isInstanceOf(
                UserNotFoundException.class
        );
    }

    @Test
    void getCurrentUserShouldRejectDisabledAccount() {

        User user = new User(
                "disabled@example.com",
                "hash",
                "Disabled",
                UserRole.CUSTOMER,
                UserStatus.DISABLED
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                userService.getCurrentUser(userId)
        ).isInstanceOf(
                AccountDisabledException.class
        );
    }

    private User activeUser() {

        return new User(
                "test@example.com",
                "hash",
                "Test User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );
    }
}