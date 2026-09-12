package com.orderflow.order.idempotency;

import com.orderflow.order.dto.CreateOrderRequest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class OrderRequestHasher {

    public String hash(
            CreateOrderRequest request
    ) {

        String canonical =
                request.items()
                        .stream()
                        .sorted(
                                (left, right) ->
                                        left.productId()
                                                .compareTo(
                                                        right.productId()
                                                )
                        )
                        .map(
                                item ->
                                        item.productId()
                                                + ":"
                                                + item.quantity()
                        )
                        .reduce(
                                (left, right) ->
                                        left + "|" + right
                        )
                        .orElse("");

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (
                NoSuchAlgorithmException exception
        ) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}