package com.orderflow.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.Resource;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.charset.StandardCharsets;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public RSAPublicKey publicKey(
            @Value("${security.jwt.public-key-location}")
            Resource publicKeyResource
    ) throws Exception {

        String key =
                publicKeyResource
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        )
                        .replace(
                                "-----BEGIN PUBLIC KEY-----",
                                ""
                        )
                        .replace(
                                "-----END PUBLIC KEY-----",
                                ""
                        )
                        .replaceAll(
                                "\\s",
                                ""
                        );

        byte[] decoded =
                Base64.getDecoder()
                        .decode(key);

        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(decoded);

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return (RSAPublicKey)
                keyFactory.generatePublic(keySpec);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            @Value("${security.jwt.issuer}")
            String issuer
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withPublicKey(publicKey)
                        .build();

        decoder.setJwtValidator(
                JwtValidators
                        .createDefaultWithIssuer(
                                issuer
                        )
        );

        return decoder;
    }
}