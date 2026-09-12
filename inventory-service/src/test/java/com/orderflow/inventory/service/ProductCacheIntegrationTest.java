package com.orderflow.inventory.service;

import com.orderflow.inventory.cache.CacheNames;
import com.orderflow.inventory.domain.Product;
import com.orderflow.inventory.dto.product.ProductResponse;
import com.orderflow.inventory.dto.product.UpdateProductRequest;
import com.orderflow.inventory.repository.ProductRepository;
import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.RedisCache;

import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
        properties = {
                "orderflow.cache.products.ttl=PT1S"
        }
)
class ProductCacheIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Cache productCache;

    private RedisCache redisCache;

    @BeforeEach
    void setUp() {

        productCache =
                cacheManager.getCache(
                        CacheNames.PRODUCTS
                );

        assertThat(
                productCache
        ).isNotNull();

        redisCache =
                unwrapRedisCache(
                        productCache
                );

        redisCache.clear();
    }

    @Test
    void shouldCacheProductById() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNull();

        ProductResponse response =
                productService.getProduct(
                        productId
                );

        assertThat(
                response.id()
        ).isEqualTo(
                productId
        );

        awaitCachePresent(
                productId
        );

        Cache.ValueWrapper cached =
                redisCache.get(
                        productId
                );

        assertThat(
                cached
        ).isNotNull();

        assertThat(
                cached.get()
        ).isInstanceOf(
                ProductResponse.class
        );

        ProductResponse cachedProduct =
                (ProductResponse)
                        cached.get();

        assertThat(
                cachedProduct.id()
        ).isEqualTo(
                productId
        );

        assertThat(
                cachedProduct.sku()
        ).isEqualTo(
                product.getSku()
        );
    }

    @Test
    void shouldReturnProductFromCacheOnSecondLookup() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        ProductResponse firstResponse =
                productService.getProduct(
                        productId
                );

        awaitCachePresent(
                productId
        );

        productRepository.deleteById(
                productId
        );

        productRepository.flush();

        assertThat(
                productRepository.findById(
                        productId
                )
        ).isEmpty();

        ProductResponse secondResponse =
                productService.getProduct(
                        productId
                );

        assertThat(
                secondResponse.id()
        ).isEqualTo(
                firstResponse.id()
        );

        assertThat(
                secondResponse.sku()
        ).isEqualTo(
                firstResponse.sku()
        );

        assertThat(
                secondResponse.name()
        ).isEqualTo(
                firstResponse.name()
        );
    }

    @Test
    void shouldEvictCacheWhenProductUpdated() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        productService.getProduct(
                productId
        );

        awaitCachePresent(
                productId
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNotNull();

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "Updated Cache Product",
                        "Updated cache product description",
                        new BigDecimal(
                                "1299.00"
                        )
                );

        ProductResponse updated =
                productService.updateProduct(
                        productId,
                        request
                );

        assertThat(
                updated.name()
        ).isEqualTo(
                "Updated Cache Product"
        );

        assertThat(
                updated.price()
        ).isEqualByComparingTo(
                "1299.00"
        );

        awaitCacheAbsent(
                productId
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNull();

        ProductResponse reloaded =
                productService.getProduct(
                        productId
                );

        assertThat(
                reloaded.name()
        ).isEqualTo(
                "Updated Cache Product"
        );

        assertThat(
                reloaded.price()
        ).isEqualByComparingTo(
                "1299.00"
        );
    }

    @Test
    void shouldEvictCacheWhenProductDeactivated() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        productService.getProduct(
                productId
        );

        awaitCachePresent(
                productId
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNotNull();

        productService.deactivateProduct(
                productId
        );

        awaitCacheAbsent(
                productId
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNull();

        Product deactivated =
                productRepository
                        .findById(
                                productId
                        )
                        .orElseThrow();

        assertThat(
                deactivated.isActive()
        ).isFalse();
    }

    @Test
    void shouldExpireCachedProductAfterTtl() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        productService.getProduct(
                productId
        );

        awaitCachePresent(
                productId
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNotNull();

        awaitCondition(
                () ->
                        redisCache.get(
                                productId
                        ) == null,
                Duration.ofSeconds(
                        5
                )
        );

        assertThat(
                redisCache.get(
                        productId
                )
        ).isNull();
    }

    @Test
    void shouldCollectCacheHitAndMissStatistics() {

        Product product =
                createProduct();

        UUID productId =
                product.getId();

        redisCache.evict(
                productId
        );

        CacheStatistics before =
                redisCache.getStatistics();

        long initialHits =
                before.getHits();

        long initialMisses =
                before.getMisses();

        productService.getProduct(
                productId
        );

        productService.getProduct(
                productId
        );

        CacheStatistics after =
                redisCache.getStatistics();

        assertThat(
                after.getMisses()
        ).isGreaterThan(
                initialMisses
        );

        assertThat(
                after.getHits()
        ).isGreaterThan(
                initialHits
        );
    }

    private Product createProduct() {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(
                                0,
                                8
                        );

        Product product =
                new Product(
                        "CACHE-" + suffix,
                        "Cache Product " + suffix,
                        "Product used for cache integration testing",
                        new BigDecimal(
                                "999.00"
                        )
                );

        return productRepository
                .saveAndFlush(
                        product
                );
    }

    private RedisCache unwrapRedisCache(
            Cache cache
    ) {

        Cache target =
                cache;

        if (
                target
                        instanceof TransactionAwareCacheDecorator decorator
        ) {

            target =
                    decorator.getTargetCache();
        }

        assertThat(
                target
        ).isInstanceOf(
                RedisCache.class
        );

        return (RedisCache)
                target;
    }

    private void awaitCachePresent(
            UUID productId
    ) {

        awaitCondition(
                () ->
                        redisCache.get(
                                productId
                        ) != null,
                Duration.ofSeconds(
                        5
                )
        );
    }

    private void awaitCacheAbsent(
            UUID productId
    ) {

        awaitCondition(
                () ->
                        redisCache.get(
                                productId
                        ) == null,
                Duration.ofSeconds(
                        5
                )
        );
    }

    private void awaitCondition(
            BooleanSupplier condition,
            Duration timeout
    ) {

        long deadline =
                System.nanoTime()
                        + timeout.toNanos();

        while (
                System.nanoTime()
                        < deadline
        ) {

            if (
                    condition.getAsBoolean()
            ) {
                return;
            }

            try {

                Thread.sleep(
                        25
                );

            } catch (
                    InterruptedException exception
            ) {

                Thread.currentThread()
                        .interrupt();

                throw new AssertionError(
                        exception
                );
            }
        }

        assertThat(
                condition.getAsBoolean()
        ).isTrue();
    }
}