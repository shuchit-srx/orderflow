package com.orderflow.user.controller;

import com.orderflow.user.dto.user.UpdateUserRequest;
import com.orderflow.user.dto.user.UserResponse;
import com.orderflow.user.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId = extractUserId(jwt);

        UserResponse response =
                userService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UUID userId = extractUserId(jwt);

        UserResponse response =
                userService.updateCurrentUser(
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Jwt jwt) {

        String subject = Objects.requireNonNull(
                jwt.getSubject(),
                "JWT subject must not be null"
        );

        return UUID.fromString(subject);
    }
}