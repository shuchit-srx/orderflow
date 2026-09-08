CREATE TABLE products (
                          id UUID PRIMARY KEY,

                          sku VARCHAR(100) NOT NULL UNIQUE,

                          name VARCHAR(200) NOT NULL,

                          description TEXT,

                          price NUMERIC(19, 2) NOT NULL,

                          active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at TIMESTAMPTZ NOT NULL,

                          updated_at TIMESTAMPTZ NOT NULL,

                          CONSTRAINT chk_products_price
                              CHECK (price >= 0)
);

CREATE INDEX idx_products_active
    ON products(active);

CREATE INDEX idx_products_name
    ON products(name);


CREATE TABLE inventory (
                           product_id UUID PRIMARY KEY,

                           available_quantity INTEGER NOT NULL DEFAULT 0,

                           reserved_quantity INTEGER NOT NULL DEFAULT 0,

                           updated_at TIMESTAMPTZ NOT NULL,

                           CONSTRAINT fk_inventory_product
                               FOREIGN KEY (product_id)
                                   REFERENCES products(id),

                           CONSTRAINT chk_inventory_available
                               CHECK (available_quantity >= 0),

                           CONSTRAINT chk_inventory_reserved
                               CHECK (reserved_quantity >= 0)
);