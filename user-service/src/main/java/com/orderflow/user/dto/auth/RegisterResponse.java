package com.orderflow.user.dto.auth;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        UserStatus status
) {

    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus()
        );
    }
}