package com.orderflow.user.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @NotBlank(message = "Password is required")
        String password

) {

        public LoginRequest {
                if (email != null) {
                        email = email
                                .trim()
                                .toLowerCase(Locale.ROOT);
                }
        }
}