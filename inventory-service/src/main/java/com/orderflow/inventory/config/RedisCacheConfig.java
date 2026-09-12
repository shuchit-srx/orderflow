package com.orderflow.inventory.config;

import com.orderflow.inventory.cache.CacheNames;
import com.orderflow.inventory.cache.LoggingCacheErrorHandler;
import com.orderflow.inventory.dto.product.ProductResponse;

import org.springframework.beans.factory.annotation.Value;

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

import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private final Duration productTtl;

    public RedisCacheConfig(
            @Value(
                    "${orderflow.cache.products.ttl:PT10M}"
            )
            Duration productTtl
    ) {
        this.productTtl = productTtl;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisSerializationContext.SerializationPair<String>
                keySerialization =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                new StringRedisSerializer()
                        );

        JacksonJsonRedisSerializer<ProductResponse>
                productSerializer =
                new JacksonJsonRedisSerializer<>(
                        objectMapper,
                        ProductResponse.class
                );

        RedisSerializationContext.SerializationPair<ProductResponse>
                productValueSerialization =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                productSerializer
                        );

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .disableCachingNullValues()
                        .serializeKeysWith(
                                keySerialization
                        )
                        .computePrefixWith(
                                cacheName ->
                                        "orderflow:inventory:"
                                                + cacheName
                                                + "::"
                        );

        RedisCacheConfiguration productConfiguration =
                defaultConfiguration
                        .entryTtl(
                                productTtl
                        )
                        .serializeValuesWith(
                                productValueSerialization
                        );

        return RedisCacheManager
                .builder(
                        connectionFactory
                )
                .cacheDefaults(
                        defaultConfiguration
                )
                .withInitialCacheConfigurations(
                        Map.of(
                                CacheNames.PRODUCTS,
                                productConfiguration
                        )
                )
                .transactionAware()
                .enableStatistics()
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}