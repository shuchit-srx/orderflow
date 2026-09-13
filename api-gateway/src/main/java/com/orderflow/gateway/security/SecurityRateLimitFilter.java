package com.orderflow.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public class SecurityRateLimitFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SecurityRateLimitFilter.class
            );

    private static final DefaultRedisScript<Long>
            RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT =
                new DefaultRedisScript<>();

        RATE_LIMIT_SCRIPT.setScriptText(
                """
                local current = redis.call('INCR', KEYS[1])
                local ttl = redis.call('TTL', KEYS[1])

                if current == 1 or ttl < 0 then
                    redis.call(
                        'EXPIRE',
                        KEYS[1],
                        ARGV[1]
                    )
                end

                return current
                """
        );

        RATE_LIMIT_SCRIPT.setResultType(
                Long.class
        );
    }

    private final StringRedisTemplate redisTemplate;

    private final boolean enabled;

    private final boolean trustForwardedFor;

    private final int windowSeconds;

    private final int loginLimit;

    private final int registrationLimit;

    private final int orderCreationLimit;

    public SecurityRateLimitFilter(
            StringRedisTemplate redisTemplate,
            boolean enabled,
            boolean trustForwardedFor,
            int windowSeconds,
            int loginLimit,
            int registrationLimit,
            int orderCreationLimit
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.windowSeconds = windowSeconds;
        this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit;
        this.orderCreationLimit = orderCreationLimit;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        RateLimitRule rule =
                resolveRule(request);

        if (rule == null) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        String identity =
                resolveIdentity(
                        request,
                        rule.identityType()
                );

        String identityHash =
                sha256(identity);

        String redisKey =
                "orderflow:rate-limit:"
                        + rule.name()
                        + ":"
                        + identityHash;

        long redisStartNanos =
                System.nanoTime();

        log.info(
                "Rate-limit Redis check starting. rule={}",
                rule.name()
        );

        try {

            Long currentCount =
                    redisTemplate.execute(
                            RATE_LIMIT_SCRIPT,
                            List.of(redisKey),
                            String.valueOf(
                                    windowSeconds
                            )
                    );

            long redisElapsedMs =
                    (System.nanoTime()
                            - redisStartNanos)
                            / 1_000_000;

            log.info(
                    "Rate-limit Redis check completed. "
                            + "rule={} elapsedMs={} count={}",
                    rule.name(),
                    redisElapsedMs,
                    currentCount
            );

            if (currentCount > rule.limit()) {

                log.info(
                        "Rate limit exceeded. "
                                + "rule={} count={} limit={}",
                        rule.name(),
                        currentCount,
                        rule.limit()
                );

                rejectRequest(response);
                return;
            }

        } catch (Exception ex) {

            long redisElapsedMs =
                    (System.nanoTime()
                            - redisStartNanos)
                            / 1_000_000;

            /*
             * Fail open deliberately.
             */
            log.warn(
                    "Rate limiter unavailable; "
                            + "allowing request. "
                            + "rule={} elapsedMs={} "
                            + "exception={} message={}",
                    rule.name(),
                    redisElapsedMs,
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private RateLimitRule resolveRule(
            HttpServletRequest request
    ) {

        if (!"POST".equalsIgnoreCase(
                request.getMethod()
        )) {
            return null;
        }

        String path =
                request.getRequestURI();

        return switch (path) {

            case "/api/v1/auth/login" ->
                    new RateLimitRule(
                            "login",
                            loginLimit,
                            IdentityType.IP
                    );

            case "/api/v1/auth/register" ->
                    new RateLimitRule(
                            "registration",
                            registrationLimit,
                            IdentityType.IP
                    );

            case "/api/v1/orders" ->
                    new RateLimitRule(
                            "order-create",
                            orderCreationLimit,
                            IdentityType.PRINCIPAL
                    );

            default -> null;
        };
    }

    private String resolveIdentity(
            HttpServletRequest request,
            IdentityType identityType
    ) {

        if (identityType
                == IdentityType.PRINCIPAL) {

            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !authentication
                    .getName()
                    .isBlank()) {

                return "user:"
                        + authentication
                        .getName();
            }
        }

        return "ip:"
                + resolveClientIp(request);
    }

    private String resolveClientIp(
            HttpServletRequest request
    ) {

        if (trustForwardedFor) {

            String forwardedFor =
                    request.getHeader(
                            "X-Forwarded-For"
                    );

            if (forwardedFor != null
                    && !forwardedFor.isBlank()) {

                String[] addresses =
                        forwardedFor.split(",");

                return addresses[
                        addresses.length - 1
                        ].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void rejectRequest(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(
                HttpStatus
                        .TOO_MANY_REQUESTS
                        .value()
        );

        response.setHeader(
                "Retry-After",
                String.valueOf(
                        windowSeconds
                )
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.getWriter().write(
                """
                {
                  "status": 429,
                  "error": "RATE_LIMIT_EXCEEDED",
                  "message": "Too many requests"
                }
                """
        );
    }

    private String sha256(
            String value
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    ex
            );
        }
    }

    private record RateLimitRule(
            String name,
            int limit,
            IdentityType identityType
    ) {
    }

    private enum IdentityType {
        IP,
        PRINCIPAL
    }
}