package com.orderflow.inventory.config;

import com.orderflow.inventory.security.InternalServiceAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@EnableConfigurationProperties(
        InternalServiceAuthProperties.class
)
@Configuration
public class SecurityConfig {

    private final InternalServiceAuthProperties
            internalServiceAuthProperties;

    public SecurityConfig(
            InternalServiceAuthProperties internalServiceAuthProperties
    ) {
        this.internalServiceAuthProperties =
                internalServiceAuthProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {

        InternalServiceAuthenticationFilter internalFilter =
                new InternalServiceAuthenticationFilter(
                        internalServiceAuthProperties
                );

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Health endpoint
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        )
                        .permitAll()

                        /*
                         * Public product reads
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
                         * Public inventory reads
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
                         * Internal service-to-service APIs
                         */
                        .requestMatchers(
                                "/api/v1/internal/**"
                        )
                        .hasRole("SERVICE")

                        /*
                         * Admin APIs
                         */
                        .requestMatchers(
                                "/api/v1/admin/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * Everything else requires authentication.
                         */
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                /*
                 * Existing internal-service authentication.
                 * Keep this before BearerTokenAuthenticationFilter.
                 */
                .addFilterBefore(
                        internalFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }
}