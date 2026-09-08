package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Optional<Product> findByIdAndActiveTrue(UUID id);
}