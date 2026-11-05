
CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    shipment_code VARCHAR(50) NOT NULL UNIQUE,

    carrier VARCHAR(100),
    tracking_number VARCHAR(150),

    status VARCHAR(30) NOT NULL DEFAULT 'PREPARING'
        CHECK (
            status IN (
                'PREPARING',
                'SHIPPED',
                'DELIVERED',
                'DELIVERY_FAILED',
                'RETURNED'
            )
        ),

    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT
);

CREATE TABLE shipment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    shipment_id UUID NOT NULL,
    order_item_id UUID NOT NULL,

    quantity INTEGER NOT NULL CHECK (quantity > 0),

    CONSTRAINT fk_shipment_items_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_shipment_items_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_shipment_order_item
        UNIQUE (shipment_id, order_item_id)
);

CREATE INDEX idx_shipments_order_id
    ON shipments(order_id);

CREATE INDEX idx_shipments_tracking_number
    ON shipments(tracking_number);

CREATE INDEX idx_shipment_items_order_item_id
    ON shipment_items(order_item_id);
