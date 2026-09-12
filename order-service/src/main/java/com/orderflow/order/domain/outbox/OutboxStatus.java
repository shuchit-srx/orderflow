package com.orderflow.order.domain.outbox;

public enum OutboxStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}