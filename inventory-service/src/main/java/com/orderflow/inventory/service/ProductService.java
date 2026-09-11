package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.Product;
import com.orderflow.inventory.dto.product.ProductPageResponse;
import com.orderflow.inventory.dto.product.ProductResponse;
import com.orderflow.inventory.exception.ProductNotFoundException;
import com.orderflow.inventory.repository.ProductRepository;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.product.CreateProductRequest;
import com.orderflow.inventory.dto.product.UpdateProductRequest;
import com.orderflow.inventory.exception.SkuAlreadyExistsException;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.config.RedisCacheConfig;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "name",
                    "price",
                    "createdAt"
            );

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository
    ) {
        this.productRepository =
                productRepository;

        this.inventoryRepository =
                inventoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(
            CreateProductRequest request
    ) {

        if (productRepository.existsBySku(
                request.sku()
        )) {

            throw new SkuAlreadyExistsException(
                    request.sku()
            );
        }

        Product product =
                new Product(
                        request.sku(),
                        request.name(),
                        request.description(),
                        request.price()
                );

        Product savedProduct;

        try {

            /*
             * Flush here so a concurrent duplicate
             * SKU violation happens inside this method.
             */
            savedProduct =
                    productRepository
                            .saveAndFlush(
                                    product
                            );

        } catch (DataIntegrityViolationException exception) {

            throw new SkuAlreadyExistsException(
                    request.sku()
            );
        }

        /*
         * Every product starts with zero inventory.
         */
        Inventory inventory =
                new Inventory(
                        savedProduct.getId()
                );

        inventoryRepository.save(
                inventory
        );

        return ProductResponse.from(
                savedProduct
        );
    }

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.clamp(
                        size,
                        1,
                        MAX_PAGE_SIZE
                );

        validatePriceRange(
                minPrice,
                maxPrice
        );

        String safeSortField =
                ALLOWED_SORT_FIELDS.contains(sortBy)
                        ? sortBy
                        : "name";

        Sort.Direction direction =
                "desc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        PageRequest pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                direction,
                                safeSortField
                        )
                );

        String normalizedName =
                normalizeNullable(name);

        Specification<Product> specification =
                buildSpecification(
                        normalizedName,
                        minPrice,
                        maxPrice
                );

        Page<ProductResponse> products =
                productRepository
                        .findAll(
                                specification,
                                pageable
                        )
                        .map(
                                ProductResponse::from
                        );

        return ProductPageResponse.from(
                products
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.PRODUCT_BY_ID,
            key = "#productId"
    )
    public ProductResponse getProduct(
            UUID productId
    ) {

        Product product =
                productRepository
                        .findByIdAndActiveTrue(productId)
                        .orElseThrow(() ->
                                new ProductNotFoundException(
                                        productId
                                )
                        );

        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(
            cacheNames = RedisCacheConfig.PRODUCT_BY_ID,
            key = "#productId"
    )
    public ProductResponse updateProduct(
            UUID productId,
            UpdateProductRequest request
    ) {

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new ProductNotFoundException(
                                        productId
                                )
                        );

        /*
         * Logically deleted products aren't
         * editable in V1.
         */
        if (!product.isActive()) {

            throw new ProductNotFoundException(
                    productId
            );
        }

        product.updateDetails(
                request.name(),
                request.description(),
                request.price()
        );

        /*
         * Explicit save isn't technically necessary
         * because this is a managed JPA entity,
         * but we'll keep repository semantics clear.
         */
        Product updated =
                productRepository.save(
                        product
                );

        return ProductResponse.from(
                updated
        );
    }

    @Transactional
    @CacheEvict(
            cacheNames = RedisCacheConfig.PRODUCT_BY_ID,
            key = "#productId"
    )
    public void deactivateProduct(
            UUID productId
    ) {

        Product product =
                productRepository
                        .findByIdAndActiveTrue(productId)
                        .orElseThrow(() ->
                                new ProductNotFoundException(
                                        productId
                                )
                        );

        product.deactivate();

        productRepository.save(product);
    }

    private Specification<Product> buildSpecification(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            /*
             * Public catalog must only show
             * active products.
             */
            predicates.add(
                    criteriaBuilder.isTrue(
                            root.get("active")
                    )
            );

            if (name != null) {

                String pattern =
                        "%"
                                + name.toLowerCase(
                                Locale.ROOT
                        )
                                + "%";

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("name")
                                ),
                                pattern
                        )
                );
            }

            if (minPrice != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("price"),
                                minPrice
                        )
                );
            }

            if (maxPrice != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("price"),
                                maxPrice
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }

    private void validatePriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        if (minPrice != null
                && minPrice.signum() < 0) {

            throw new IllegalArgumentException(
                    "Minimum price cannot be negative"
            );
        }

        if (maxPrice != null
                && maxPrice.signum() < 0) {

            throw new IllegalArgumentException(
                    "Maximum price cannot be negative"
            );
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new IllegalArgumentException(
                    "Minimum price cannot exceed maximum price"
            );
        }
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}