package com.orderflow.inventory.security;

import com.orderflow.inventory.config.InternalServiceAuthProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.List;

public class InternalServiceAuthenticationFilter
        extends OncePerRequestFilter {

    public static final String HEADER_NAME =
            "X-Internal-Service-Token";

    private final String expectedToken;

    public InternalServiceAuthenticationFilter(
            InternalServiceAuthProperties properties
    ) {

        if (
                properties.token() == null
                        || properties.token().isBlank()
        ) {

            throw new IllegalStateException(
                    "security.internal.token must be configured"
            );
        }

        this.expectedToken =
                properties.token();
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return !request
                .getRequestURI()
                .startsWith(
                        "/api/v1/internal/"
                );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException,
            IOException {

        String providedToken =
                request.getHeader(
                        HEADER_NAME
                );

        if (
                !tokensMatch(
                        expectedToken,
                        providedToken
                )
        ) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json"
            );

            response
                    .getWriter()
                    .write(
                            """
                            {
                              "error":"INVALID_INTERNAL_SERVICE_TOKEN",
                              "message":"Internal service authentication failed"
                            }
                            """
                    );

            return;
        }

        UsernamePasswordAuthenticationToken
                authentication =
                new UsernamePasswordAuthenticationToken(
                        "order-service",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SERVICE"
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder
                        .createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder
                .setContext(
                        context
                );

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            SecurityContextHolder
                    .clearContext();
        }
    }

    private boolean tokensMatch(
            String expected,
            String provided
    ) {

        if (
                provided == null
                        || provided.isBlank()
        ) {

            return false;
        }

        byte[] expectedBytes =
                expected.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] providedBytes =
                provided.getBytes(
                        StandardCharsets.UTF_8
                );

        return MessageDigest.isEqual(
                expectedBytes,
                providedBytes
        );
    }
}