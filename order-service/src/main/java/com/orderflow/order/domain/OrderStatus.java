package com.orderflow.order.domain;

public enum OrderStatus {

    CREATED,
    INVENTORY_RESERVED,
    CONFIRMED,
    CANCELLED,
    FAILED
}