package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    Optional<Product> findByIdAndActiveTrue(
            UUID id
    );

    boolean existsByIdAndActiveTrue(
            UUID id
    );
}