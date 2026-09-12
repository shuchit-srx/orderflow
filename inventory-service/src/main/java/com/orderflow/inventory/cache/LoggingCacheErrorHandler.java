package com.orderflow.inventory.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    LoggingCacheErrorHandler.class
            );

    @Override
    public void handleCacheGetError(
            RuntimeException exception,
            Cache cache,
            Object key
    ) {
        log.warn(
                "Cache GET failed cache={} key={} error={}; continuing without cache",
                cache.getName(),
                key,
                exception.getMessage()
        );
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception,
            Cache cache,
            Object key,
            Object value
    ) {
        log.warn(
                "Cache PUT failed cache={} key={} error={}; database result remains authoritative",
                cache.getName(),
                key,
                exception.getMessage()
        );
    }

    @Override
    public void handleCacheEvictError(
            RuntimeException exception,
            Cache cache,
            Object key
    ) {
        log.warn(
                "Cache EVICT failed cache={} key={} error={}; cached value may remain until TTL",
                cache.getName(),
                key,
                exception.getMessage()
        );
    }

    @Override
    public void handleCacheClearError(
            RuntimeException exception,
            Cache cache
    ) {
        log.warn(
                "Cache CLEAR failed cache={} error={}; cached values may remain until TTL",
                cache.getName(),
                exception.getMessage()
        );
    }
}