package com.orderflow.user.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}