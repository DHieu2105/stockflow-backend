
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_code VARCHAR(50) NOT NULL UNIQUE,

    customer_id UUID NOT NULL,
    fulfillment_warehouse_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (
            status IN (
                'PENDING_PAYMENT',
                'CONFIRMED',
                'PROCESSING',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED',
                'DELIVERY_FAILED',
                'RETURNED'
            )
        ),

    subtotal NUMERIC(15, 2) NOT NULL CHECK (subtotal >= 0),
    shipping_fee NUMERIC(15, 2) NOT NULL DEFAULT 0
        CHECK (shipping_fee >= 0),
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0
        CHECK (discount_amount >= 0),
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),

    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address JSONB NOT NULL
        CHECK (jsonb_typeof(shipping_address) = 'object'),

    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_orders_warehouse
        FOREIGN KEY (fulfillment_warehouse_id)
        REFERENCES warehouses(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_orders_total
        CHECK (
            total_amount = subtotal + shipping_fee - discount_amount
        ),

    CONSTRAINT chk_orders_discount
        CHECK (discount_amount <= subtotal + shipping_fee)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,
    variant_id UUID NOT NULL,

    sku_snapshot VARCHAR(100) NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    variant_name_snapshot VARCHAR(150) NOT NULL,

    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),

    line_total NUMERIC(17, 2)
        GENERATED ALWAYS AS (quantity * unit_price) STORED,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
        ON DELETE RESTRICT
);

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,
    warehouse_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (
            status IN (
                'ACTIVE',
                'RELEASED',
                'CONSUMED',
                'EXPIRED'
            )
        ),

    expires_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_reservations_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_stock_reservations_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses(id)
        ON DELETE RESTRICT
);

CREATE TABLE stock_reservation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    reservation_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    inventory_id UUID NOT NULL,

    quantity INTEGER NOT NULL CHECK (quantity > 0),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (
            status IN (
                'ACTIVE',
                'RELEASED',
                'CONSUMED'
            )
        ),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_items_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES stock_reservations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservation_items_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservation_items_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES inventories(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_reservation_order_item
        UNIQUE (reservation_id, order_item_id)
);

CREATE INDEX idx_orders_customer_created
    ON orders(customer_id, created_at DESC);

CREATE INDEX idx_orders_status_created
    ON orders(status, created_at DESC);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX idx_stock_reservations_status_expiry
    ON stock_reservations(status, expires_at);

CREATE INDEX idx_reservation_items_inventory_id
    ON stock_reservation_items(inventory_id);
