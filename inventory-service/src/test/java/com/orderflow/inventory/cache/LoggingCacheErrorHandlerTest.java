package com.orderflow.inventory.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingCacheErrorHandlerTest {

    private LoggingCacheErrorHandler errorHandler;

    private Cache cache;

    @BeforeEach
    void setUp() {
        errorHandler =
                new LoggingCacheErrorHandler();

        cache =
                mock(
                        Cache.class
                );

        when(
                cache.getName()
        ).thenReturn(
                CacheNames.PRODUCTS
        );
    }

    @Test
    void getFailureMustNotBePropagated() {
        RuntimeException exception =
                new RuntimeException(
                        "Redis unavailable"
                );

        assertThatCode(
                () ->
                        errorHandler
                                .handleCacheGetError(
                                        exception,
                                        cache,
                                        "product-1"
                                )
        ).doesNotThrowAnyException();
    }

    @Test
    void putFailureMustNotBePropagated() {
        RuntimeException exception =
                new RuntimeException(
                        "Redis unavailable"
                );

        assertThatCode(
                () ->
                        errorHandler
                                .handleCachePutError(
                                        exception,
                                        cache,
                                        "product-1",
                                        "value"
                                )
        ).doesNotThrowAnyException();
    }

    @Test
    void evictFailureMustNotBePropagated() {
        RuntimeException exception =
                new RuntimeException(
                        "Redis unavailable"
                );

        assertThatCode(
                () ->
                        errorHandler
                                .handleCacheEvictError(
                                        exception,
                                        cache,
                                        "product-1"
                                )
        ).doesNotThrowAnyException();
    }

    @Test
    void clearFailureMustNotBePropagated() {
        RuntimeException exception =
                new RuntimeException(
                        "Redis unavailable"
                );

        assertThatCode(
                () ->
                        errorHandler
                                .handleCacheClearError(
                                        exception,
                                        cache
                                )
        ).doesNotThrowAnyException();
    }
}