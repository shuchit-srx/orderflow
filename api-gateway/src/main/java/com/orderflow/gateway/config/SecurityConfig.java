package com.orderflow.gateway.config;

import com.orderflow.gateway.security.RoleClaimConverter;
import com.orderflow.gateway.security.SecurityRateLimitFilter;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RoleClaimConverter
            roleClaimConverter;

    public SecurityConfig(
            RoleClaimConverter roleClaimConverter
    ) {
        this.roleClaimConverter =
                roleClaimConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            SecurityRateLimitFilter securityRateLimitFilter
    ) {

        http
                /*
                 * Stateless REST API.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )

                /*
                 * Browser CORS policy.
                 */
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )

                /*
                 * JWT authentication means there is
                 * no HTTP session-based security.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy
                                        .STATELESS
                        )
                )

                /*
                 * Authorization rules.
                 */
                .authorizeHttpRequests(authorize ->
                        authorize

                                /*
                                 * CORS pre-flight.
                                 */
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                /*
                                 * Health endpoints.
                                 */
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info"
                                )
                                .permitAll()

                                /*
                                 * Swagger routes.
                                 *
                                 * These remain available
                                 * locally. Production config
                                 * disables Swagger itself.
                                 */
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                /*
                                 * Authentication endpoints.
                                 */
                                .requestMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login"
                                )
                                .permitAll()

                                /*
                                 * Public product reads.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/products",
                                        "/api/v1/products/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.HEAD,
                                        "/api/v1/products",
                                        "/api/v1/products/**"
                                )
                                .permitAll()

                                /*
                                 * Public inventory reads.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/inventory",
                                        "/api/v1/inventory/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.HEAD,
                                        "/api/v1/inventory",
                                        "/api/v1/inventory/**"
                                )
                                .permitAll()

                                /*
                                 * Administrative API.
                                 */
                                .requestMatchers(
                                        "/api/v1/admin/**"
                                )
                                .hasRole("ADMIN")

                                /*
                                 * Product modifications
                                 * require ADMIN.
                                 */
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/products",
                                        "/api/v1/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/products/**"
                                )
                                .hasRole("ADMIN")

                                /*
                                 * User APIs.
                                 */
                                .requestMatchers(
                                        "/api/v1/users/**"
                                )
                                .hasAnyRole(
                                        "CUSTOMER",
                                        "ADMIN"
                                )

                                /*
                                 * Customer order API.
                                 */
                                .requestMatchers(
                                        "/api/v1/orders/**"
                                )
                                .hasRole("CUSTOMER")

                                /*
                                 * Internal service APIs must
                                 * never be exposed through
                                 * the public Gateway.
                                 */
                                .requestMatchers(
                                        "/internal/**",
                                        "/api/v1/internal/**"
                                )
                                .denyAll()

                                /*
                                 * Fail closed.
                                 */
                                .anyRequest()
                                .denyAll()
                )

                /*
                 * Security response headers.
                 */
                .headers(headers ->
                        headers

                                .frameOptions(frame ->
                                        frame.deny()
                                )

                                .contentSecurityPolicy(csp ->
                                        csp.policyDirectives(
                                                "default-src 'none'; "
                                                        + "frame-ancestors 'none'; "
                                                        + "base-uri 'none'"
                                        )
                                )

                                .referrerPolicy(referrer ->
                                        referrer.policy(
                                                ReferrerPolicyHeaderWriter
                                                        .ReferrerPolicy
                                                        .NO_REFERRER
                                        )
                                )

                                .permissionsPolicyHeader(
                                        permissions ->
                                                permissions.policy(
                                                        "camera=(), "
                                                                + "microphone=(), "
                                                                + "geolocation=()"
                                                )
                                )

                                /*
                                 * HSTS appears only for
                                 * HTTPS requests.
                                 */
                                .httpStrictTransportSecurity(
                                        hsts ->
                                                hsts
                                                        .includeSubDomains(
                                                                true
                                                        )
                                                        .preload(
                                                                true
                                                        )
                                                        .maxAgeInSeconds(
                                                                31536000
                                                        )
                                )
                )

                /*
                 * Verify JWT signature and map the
                 * custom "role" claim.
                 */
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        roleClaimConverter
                                )
                        )
                )

                /*
                 * Run after bearer authentication.
                 *
                 * This allows POST /orders to use
                 * the authenticated JWT subject as
                 * the rate-limit identity.
                 */
                .addFilterAfter(
                        securityRateLimitFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    /*
     * Rate-limit filter bean.
     */
    @Bean
    public SecurityRateLimitFilter securityRateLimitFilter(
            StringRedisTemplate redisTemplate,

            @Value(
                    "${security.rate-limit.enabled:true}"
            )
            boolean enabled,

            @Value(
                    "${security.rate-limit.trust-forwarded-for:false}"
            )
            boolean trustForwardedFor,

            @Value(
                    "${security.rate-limit.window-seconds:60}"
            )
            int windowSeconds,

            @Value(
                    "${security.rate-limit.login.max-requests:10}"
            )
            int loginLimit,

            @Value(
                    "${security.rate-limit.registration.max-requests:5}"
            )
            int registrationLimit,

            @Value(
                    "${security.rate-limit.order-creation.max-requests:30}"
            )
            int orderCreationLimit
    ) {

        return new SecurityRateLimitFilter(
                redisTemplate,
                enabled,
                trustForwardedFor,
                windowSeconds,
                loginLimit,
                registrationLimit,
                orderCreationLimit
        );
    }

    /*
     * SecurityRateLimitFilter is a servlet Filter.
     *
     * Spring Boot would otherwise automatically
     * register it with Tomcat AND we are manually
     * inserting it into Spring Security.
     *
     * Disable normal servlet auto-registration so
     * it executes exactly once inside the security
     * chain.
     */
    @Bean
    public FilterRegistrationBean<SecurityRateLimitFilter>
    disableRateLimitServletRegistration(
            SecurityRateLimitFilter filter
    ) {

        FilterRegistrationBean<SecurityRateLimitFilter>
                registration =
                new FilterRegistrationBean<>(
                        filter
                );

        registration.setEnabled(false);

        return registration;
    }

    /*
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource
    corsConfigurationSource(

            @Value(
                    "${security.cors.allowed-origins:"
                            + "http://localhost:3000,"
                            + "http://localhost:5173}"
            )
            String allowedOrigins
    ) {

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "At least one CORS origin "
                            + "must be configured"
            );
        }

        if (origins.contains("*")) {
            throw new IllegalStateException(
                    "Wildcard CORS origin "
                            + "is not allowed"
            );
        }

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                origins
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "HEAD",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Idempotency-Key",
                        "X-Request-Id"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "X-Request-Id",
                        "Retry-After"
                )
        );

        configuration.setAllowCredentials(
                true
        );

        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}