package com.orderflow.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RoleClaimConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> ALLOWED_ROLES =
            Set.of(
                    "CUSTOMER",
                    "ADMIN"
            );

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String role = jwt.getClaimAsString("role");

        List<GrantedAuthority> authorities;

        if (role == null || role.isBlank()) {

            authorities = List.of();

        } else {

            String normalizedRole =
                    role.trim()
                            .toUpperCase(Locale.ROOT);

            if (ALLOWED_ROLES.contains(normalizedRole)) {

                authorities = List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + normalizedRole
                        )
                );

            } else {

                authorities = List.of();
            }
        }

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
    }
}