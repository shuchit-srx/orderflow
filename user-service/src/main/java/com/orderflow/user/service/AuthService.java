package com.orderflow.user.service;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;
import com.orderflow.user.dto.auth.RegisterRequest;
import com.orderflow.user.dto.auth.RegisterResponse;
import com.orderflow.user.exception.EmailAlreadyExistsException;
import com.orderflow.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
             * The existsByEmail check improves UX,
             * but it is not sufficient against concurrent requests.
             *
             * PostgreSQL's UNIQUE(email) constraint is the
             * authoritative protection.
             */

            throw new EmailAlreadyExistsException(email);
        }
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}