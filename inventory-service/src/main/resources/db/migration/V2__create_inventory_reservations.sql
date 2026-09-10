CREATE TABLE inventory_reservations (
                                        id UUID PRIMARY KEY,

                                        order_id UUID NOT NULL UNIQUE,

                                        request_hash VARCHAR(64) NOT NULL,

                                        status VARCHAR(20) NOT NULL,

                                        expires_at TIMESTAMPTZ NOT NULL,

                                        created_at TIMESTAMPTZ NOT NULL,

                                        updated_at TIMESTAMPTZ NOT NULL,

                                        CONSTRAINT chk_inventory_reservation_status
                                            CHECK (
                                                status IN (
                                                           'ACTIVE',
                                                           'CONFIRMED',
                                                           'RELEASED',
                                                           'EXPIRED'
                                                    )
                                                )
);


CREATE TABLE inventory_reservation_items (
                                             id UUID PRIMARY KEY,

                                             reservation_id UUID NOT NULL,

                                             product_id UUID NOT NULL,

                                             quantity INTEGER NOT NULL,

                                             CONSTRAINT fk_reservation_item_reservation
                                                 FOREIGN KEY (reservation_id)
                                                     REFERENCES inventory_reservations(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_reservation_item_product
                                                 FOREIGN KEY (product_id)
                                                     REFERENCES products(id),

                                             CONSTRAINT chk_reservation_item_quantity
                                                 CHECK (quantity > 0),

                                             CONSTRAINT uq_reservation_product
                                                 UNIQUE (reservation_id, product_id)
);


CREATE INDEX idx_inventory_reservations_order_id
    ON inventory_reservations(order_id);

CREATE INDEX idx_inventory_reservations_status
    ON inventory_reservations(status);

CREATE INDEX idx_inventory_reservations_expires_at
    ON inventory_reservations(expires_at);

CREATE INDEX idx_reservation_items_reservation_id
    ON inventory_reservation_items(reservation_id);

CREATE INDEX idx_reservation_items_product_id
    ON inventory_reservation_items(product_id);