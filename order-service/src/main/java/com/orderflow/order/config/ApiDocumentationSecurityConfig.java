package com.orderflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class ApiDocumentationSecurityConfig {

    @Bean
    public WebSecurityCustomizer apiDocumentationWebSecurityCustomizer() {
        return web ->
                web.ignoring()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/openapi/**"
                        );
    }
}