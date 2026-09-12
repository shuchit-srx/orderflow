ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(200),
    ADD COLUMN request_hash VARCHAR(64);

ALTER TABLE orders
    ADD CONSTRAINT uq_orders_customer_idempotency
        UNIQUE (
                customer_id,
                idempotency_key
            );

CREATE INDEX idx_orders_idempotency_key
    ON orders (idempotency_key);