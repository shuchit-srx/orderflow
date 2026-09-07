package com.orderflow.user.service;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;
import com.orderflow.user.dto.auth.LoginRequest;
import com.orderflow.user.dto.auth.LoginResponse;
import com.orderflow.user.dto.auth.RegisterRequest;
import com.orderflow.user.dto.auth.RegisterResponse;
import com.orderflow.user.exception.AccountDisabledException;
import com.orderflow.user.exception.EmailAlreadyExistsException;
import com.orderflow.user.exception.InvalidCredentialsException;
import com.orderflow.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void registerShouldCreateActiveCustomerWithHashedPassword() {

        RegisterRequest request = new RegisterRequest(
                "  TEST@EXAMPLE.COM  ",
                "StrongPassword123!",
                "  Test User  "
        );

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123!"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response =
                authService.register(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertThat(savedUser.getEmail())
                .isEqualTo("test@example.com");

        assertThat(savedUser.getDisplayName())
                .isEqualTo("Test User");

        assertThat(savedUser.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(savedUser.getRole())
                .isEqualTo(UserRole.CUSTOMER);

        assertThat(savedUser.getStatus())
                .isEqualTo(UserStatus.ACTIVE);

        assertThat(response.email())
                .isEqualTo("test@example.com");

        verify(passwordEncoder)
                .encode("StrongPassword123!");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "StrongPassword123!",
                "Test User"
        );

        assertThatThrownBy(() ->
                authService.register(request)
        ).isInstanceOf(
                EmailAlreadyExistsException.class
        );

        verify(userRepository, never())
                .save(any());

        verifyNoInteractions(
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void loginShouldReturnJwtForCorrectCredentials() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "StrongPassword123!",
                "hashed-password"
        )).thenReturn(true);

        Instant issuedAt = Instant.now();
        Instant expiresAt =
                issuedAt.plusSeconds(3600);

        Jwt jwt = Jwt
                .withTokenValue("access-token")
                .header("alg", "RS256")
                .claim("sub", "test-user")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        when(jwtService.generateAccessToken(user))
                .thenReturn(jwt);

        LoginResponse response =
                authService.login(
                        new LoginRequest(
                                "test@example.com",
                                "StrongPassword123!"
                        )
                );

        assertThat(response.accessToken())
                .isEqualTo("access-token");

        assertThat(response.tokenType())
                .isEqualTo("Bearer");

        assertThat(response.expiresIn())
                .isEqualTo(3600);
    }

    @Test
    void loginShouldRejectWrongPassword() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "hashed-password"
        )).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(
                        new LoginRequest(
                                "test@example.com",
                                "wrong-password"
                        )
                )
        ).isInstanceOf(
                InvalidCredentialsException.class
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void loginShouldRejectUnknownEmail() {

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(
                        new LoginRequest(
                                "missing@example.com",
                                "SomePassword"
                        )
                )
        ).isInstanceOf(
                InvalidCredentialsException.class
        );

        verifyNoInteractions(
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void loginShouldRejectDisabledAccount() {

        User user = new User(
                "disabled@example.com",
                "hashed-password",
                "Disabled",
                UserRole.CUSTOMER,
                UserStatus.DISABLED
        );

        when(userRepository.findByEmail(
                "disabled@example.com"
        )).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.login(
                        new LoginRequest(
                                "disabled@example.com",
                                "SomePassword"
                        )
                )
        ).isInstanceOf(
                AccountDisabledException.class
        );

        verifyNoInteractions(
                passwordEncoder,
                jwtService
        );
    }
}