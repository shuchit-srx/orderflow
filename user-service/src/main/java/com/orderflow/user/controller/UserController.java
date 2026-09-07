package com.orderflow.user.controller;

import com.orderflow.user.dto.user.UserResponse;
import com.orderflow.user.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        String subject = Objects.requireNonNull(
                jwt.getSubject(),
                "JWT subject must not be null"
        );

        UUID userId = UUID.fromString(subject);

        UserResponse response =
                userService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }
}