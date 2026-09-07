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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                email,
                passwordHash,
                request.displayName().trim(),
                UserRole.CUSTOMER,
                UserStatus.ACTIVE
        );

        try {
            User savedUser = userRepository.save(user);

            return RegisterResponse.from(savedUser);

        } catch (DataIntegrityViolationException exception) {

            /*
             * The database UNIQUE constraint is the final
             * protection against two concurrent registrations
             * using the same email.
             */
            throw new EmailAlreadyExistsException(email);
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDisabledException();
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        Jwt jwt =
                jwtService.generateAccessToken(user);

        long expiresIn =
                jwt.getExpiresAt().getEpochSecond()
                        - jwt.getIssuedAt().getEpochSecond();

        return new LoginResponse(
                jwt.getTokenValue(),
                "Bearer",
                expiresIn
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}