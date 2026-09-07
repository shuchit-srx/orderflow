package com.orderflow.user.service;

import com.orderflow.user.domain.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenTtlSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,

            @Value("${security.jwt.issuer}")
            String issuer,

            @Value("${security.jwt.access-token-ttl-seconds}")
            long accessTokenTtlSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtlSeconds =
                accessTokenTtlSeconds;
    }

    public Jwt generateAccessToken(User user) {

        Instant now = Instant.now();
        Instant expiresAt =
                now.plusSeconds(accessTokenTtlSeconds);

        JwtClaimsSet claims = JwtClaimsSet
                .builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        );
    }
}