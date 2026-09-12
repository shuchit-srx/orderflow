package com.orderflow.inventory.service;

import com.orderflow.inventory.cache.CacheNames;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.domain.Product;

import com.orderflow.inventory.dto.product.CreateProductRequest;
import com.orderflow.inventory.dto.product.ProductPageResponse;
import com.orderflow.inventory.dto.product.ProductResponse;
import com.orderflow.inventory.dto.product.UpdateProductRequest;

import com.orderflow.inventory.exception.ProductNotFoundException;

import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.ProductRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

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

    @Transactional(
            readOnly = true
    )
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
                Math.max(
                        page,
                        0
                );

        int safeSize =
                Math.clamp(
                        size,
                        1,
                        100
                );

        Sort.Direction direction =
                "desc".equalsIgnoreCase(
                        sortDirection
                )
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        String safeSortBy =
                resolveSortField(
                        sortBy
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                direction,
                                safeSortBy
                        )
                );

        Specification<Product> specification =
                buildSpecification(
                        name,
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

    @Cacheable(
            cacheNames = CacheNames.PRODUCTS,
            key = "#productId",
            unless = "#result == null"
    )
    @Transactional(
            readOnly = true
    )
    public ProductResponse getProduct(
            UUID productId
    ) {
        Product product =
                productRepository
                        .findByIdAndActiveTrue(
                                productId
                        )
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                productId
                                        )
                        );

        return ProductResponse.from(
                product
        );
    }

    @Transactional
    public ProductResponse createProduct(
            CreateProductRequest request
    ) {
        Product product =
                new Product(
                        request.sku(),
                        request.name(),
                        request.description(),
                        request.price()
                );

        Product savedProduct =
                productRepository
                        .saveAndFlush(
                                product
                        );

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

    @CacheEvict(
            cacheNames = CacheNames.PRODUCTS,
            key = "#productId"
    )
    @Transactional
    public ProductResponse updateProduct(
            UUID productId,
            UpdateProductRequest request
    ) {
        Product product =
                productRepository
                        .findById(
                                productId
                        )
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                productId
                                        )
                        );

        product.updateDetails(
                request.name(),
                request.description(),
                request.price()
        );

        Product savedProduct =
                productRepository.save(
                        product
                );

        return ProductResponse.from(
                savedProduct
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.PRODUCTS,
            key = "#productId"
    )
    @Transactional
    public void deactivateProduct(
            UUID productId
    ) {
        Product product =
                productRepository
                        .findById(
                                productId
                        )
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                productId
                                        )
                        );

        product.deactivate();

        productRepository.save(
                product
        );
    }

    private Specification<Product> buildSpecification(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    criteriaBuilder.isTrue(
                            root.get(
                                    "active"
                            )
                    )
            );

            if (
                    name != null
                            && !name.isBlank()
            ) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get(
                                                "name"
                                        )
                                ),
                                "%"
                                        + name
                                        .trim()
                                        .toLowerCase()
                                        + "%"
                        )
                );
            }

            if (
                    minPrice != null
            ) {
                predicates.add(
                        criteriaBuilder
                                .greaterThanOrEqualTo(
                                        root.get(
                                                "price"
                                        ),
                                        minPrice
                                )
                );
            }

            if (
                    maxPrice != null
            ) {
                predicates.add(
                        criteriaBuilder
                                .lessThanOrEqualTo(
                                        root.get(
                                                "price"
                                        ),
                                        maxPrice
                                )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private String resolveSortField(
            String sortBy
    ) {
        if (
                sortBy == null
                        || sortBy.isBlank()
        ) {
            return "createdAt";
        }

        return switch (
                sortBy
                ) {
            case "name" ->
                    "name";

            case "price" ->
                    "price";

            case "createdAt" ->
                    "createdAt";

            case "updatedAt" ->
                    "updatedAt";

            default ->
                    "createdAt";
        };
    }
}