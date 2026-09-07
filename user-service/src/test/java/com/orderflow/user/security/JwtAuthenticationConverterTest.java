package com.orderflow.user.security;

import com.orderflow.user.config.JwtAuthenticationConverterConfig;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter =
            new JwtAuthenticationConverterConfig()
                    .jwtAuthenticationConverter();

    @Test
    void customerClaimShouldBecomeRoleCustomer() {

        Jwt jwt = createJwt("CUSTOMER");

        Authentication authentication =
                converter.convert(jwt);

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains(
                        "ROLE_CUSTOMER",
                        FactorGrantedAuthority.BEARER_AUTHORITY
                );
    }

    @Test
    void adminClaimShouldBecomeRoleAdmin() {

        Jwt jwt = createJwt("ADMIN");

        Authentication authentication =
                converter.convert(jwt);

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains(
                        "ROLE_ADMIN",
                        FactorGrantedAuthority.BEARER_AUTHORITY
                );
    }

    private Jwt createJwt(String role) {

        Instant now = Instant.now();

        return Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", role)
                .build();
    }
}