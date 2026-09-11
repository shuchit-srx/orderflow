package com.orderflow.inventory.config;

import com.orderflow.inventory.cache.LoggingCacheErrorHandler;
import com.orderflow.inventory.dto.product.ProductResponse;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig
        implements CachingConfigurer {

    public static final String PRODUCT_BY_ID =
            "productById";

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,

            @Value(
                    "${inventory.cache.product-detail-ttl:10m}"
            )
            Duration productDetailTtl
    ) {

        var keySerializer =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                new StringRedisSerializer()
                        );

        var valueSerializer =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                new JacksonJsonRedisSerializer<>(
                                        ProductResponse.class
                                )
                        );

        RedisCacheConfiguration productCache =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .entryTtl(productDetailTtl)
                        .disableCachingNullValues()
                        .computePrefixWith(
                                cacheName ->
                                        "orderflow:inventory:"
                                                + cacheName
                                                + "::"
                        )
                        .serializeKeysWith(
                                keySerializer
                        )
                        .serializeValuesWith(
                                valueSerializer
                        );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(productCache)
                .withCacheConfiguration(
                        PRODUCT_BY_ID,
                        productCache
                )
                .allowCreateOnMissingCache(false)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}