package com.orderflow.user.dto.auth;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserRole;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role
) {

    public static LoginResponse from(User user) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}