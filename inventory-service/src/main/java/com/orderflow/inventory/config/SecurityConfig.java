package com.orderflow.inventory.config;

import com.orderflow.inventory.security.InternalServiceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.orderflow.inventory.config.InternalServiceAuthProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import org.springframework.http.HttpMethod;

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
            InternalServiceAuthenticationFilter
                internalFilter =
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


                .authorizeHttpRequests(
                        auth ->
                                auth

                                        .requestMatchers(
                                                "/actuator/health"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/products/**"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/inventory/**"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                "/api/v1/internal/**"
                                        )
                                        .hasRole(
                                                "SERVICE"
                                        )

                                        .requestMatchers(
                                                "/api/v1/admin/**"
                                        )
                                        .hasRole(
                                                "ADMIN"
                                        )

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
                .addFilterBefore(
                        internalFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }
}