package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.Inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRepository
        extends JpaRepository<Inventory, UUID> {
}