CREATE TABLE orders (
                        id UUID PRIMARY KEY,

                        customer_id UUID NOT NULL,

                        status VARCHAR(30) NOT NULL,

                        total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,

                        created_at TIMESTAMPTZ NOT NULL,

                        updated_at TIMESTAMPTZ NOT NULL,

                        CONSTRAINT chk_orders_total_amount
                            CHECK (total_amount >= 0),

                        CONSTRAINT chk_orders_status
                            CHECK (
                                status IN (
                                           'CREATED',
                                           'INVENTORY_RESERVED',
                                           'CONFIRMED',
                                           'CANCELLED',
                                           'FAILED'
                                    )
                                )
);


CREATE TABLE order_items (
                             id UUID PRIMARY KEY,

                             order_id UUID NOT NULL,

                             product_id UUID NOT NULL,

                             sku VARCHAR(100) NOT NULL,

                             product_name VARCHAR(200) NOT NULL,

                             unit_price NUMERIC(19, 2) NOT NULL,

                             quantity INTEGER NOT NULL,

                             line_total NUMERIC(19, 2) NOT NULL,

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT chk_order_items_unit_price
                                 CHECK (unit_price >= 0),

                             CONSTRAINT chk_order_items_quantity
                                 CHECK (quantity > 0),

                             CONSTRAINT chk_order_items_line_total
                                 CHECK (line_total >= 0)
);


CREATE INDEX idx_orders_customer_id
    ON orders(customer_id);

CREATE INDEX idx_orders_status
    ON orders(status);

CREATE INDEX idx_orders_created_at
    ON orders(created_at);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX idx_order_items_product_id
    ON order_items(product_id);