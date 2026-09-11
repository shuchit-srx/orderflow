package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.product.UpdateProductRequest;
import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCacheIntegrationTest
        extends AbstractIntegrationTest {

    private static final String PREFIX =
            "orderflow:inventory:productById::";

    @Autowired
    private ProductService productService;

    @Test
    void shouldCacheProductById() {

        String key =
                PREFIX + IPHONE_ID;

        assertThat(
                redisTemplate.hasKey(key)
        ).isFalse();

        productService.getProduct(
                IPHONE_ID
        );

        assertThat(
                redisTemplate.hasKey(key)
        ).isTrue();
    }

    @Test
    void shouldApplyProductCacheTtl() {

        String key =
                PREFIX + IPHONE_ID;

        productService.getProduct(
                IPHONE_ID
        );

        Long ttl =
                redisTemplate.getExpire(
                        key,
                        TimeUnit.SECONDS
                );

        assertThat(ttl)
                .isNotNull()
                .isPositive()
                .isLessThanOrEqualTo(600);
    }

    @Test
    void shouldEvictCacheWhenProductUpdated() {

        String key =
                PREFIX + IPHONE_ID;

        productService.getProduct(
                IPHONE_ID
        );

        assertThat(
                redisTemplate.hasKey(key)
        ).isTrue();

        productService.updateProduct(
                IPHONE_ID,
                new UpdateProductRequest(
                        "Updated iPhone",
                        "Updated description",
                        new BigDecimal("68999.00")
                )
        );

        assertThat(
                redisTemplate.hasKey(key)
        ).isFalse();
    }

    @Test
    void shouldEvictCacheWhenProductDeactivated() {

        String key =
                PREFIX + IPHONE_ID;

        productService.getProduct(
                IPHONE_ID
        );

        assertThat(
                redisTemplate.hasKey(key)
        ).isTrue();

        productService.deactivateProduct(
                IPHONE_ID
        );

        assertThat(
                redisTemplate.hasKey(key)
        ).isFalse();
    }

    @Test
    void inventoryReadShouldNotCreateRedisCacheEntry() {

        assertThat(
                redisTemplate.keys("*")
        ).isEmpty();

        jdbcTemplate.queryForObject(
                """
                SELECT available_quantity
                FROM inventory
                WHERE product_id = ?
                """,
                Integer.class,
                IPHONE_ID
        );

        assertThat(
                redisTemplate.keys("*")
        ).isEmpty();
    }
}