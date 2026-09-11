package com.orderflow.order.security;

import com.orderflow.order.exception.InvalidCustomerIdentityException;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentCustomer {

    public UUID id(Jwt jwt) {

        String subject =
                jwt.getSubject();

        if (subject == null
                || subject.isBlank()) {

            throw new InvalidCustomerIdentityException();
        }

        try {

            return UUID.fromString(
                    subject
            );

        } catch (IllegalArgumentException exception) {

            throw new InvalidCustomerIdentityException();
        }
    }
}