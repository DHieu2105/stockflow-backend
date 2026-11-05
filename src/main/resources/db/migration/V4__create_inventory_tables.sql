
CREATE TABLE inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    warehouse_id UUID NOT NULL,
    variant_id UUID NOT NULL,

    on_hand INTEGER NOT NULL DEFAULT 0,
    reserved INTEGER NOT NULL DEFAULT 0,

    version BIGINT NOT NULL DEFAULT 0,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventories_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_inventories_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_inventories_warehouse_variant
        UNIQUE (warehouse_id, variant_id),

    CONSTRAINT chk_inventories_on_hand
        CHECK (on_hand >= 0),

    CONSTRAINT chk_inventories_reserved
        CHECK (reserved >= 0),

    CONSTRAINT chk_inventories_reserved_limit
        CHECK (reserved <= on_hand)
);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    inventory_id UUID NOT NULL,

    movement_type VARCHAR(30) NOT NULL
        CHECK (
            movement_type IN (
                'INBOUND',
                'OUTBOUND',
                'ADJUSTMENT',
                'TRANSFER_IN',
                'TRANSFER_OUT'
            )
        ),

    quantity INTEGER NOT NULL CHECK (quantity > 0),

    quantity_before INTEGER NOT NULL CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CHECK (quantity_after >= 0),

    reference_type VARCHAR(50),
    reference_id UUID,

    note TEXT,

    created_by UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_movements_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES inventories(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_stock_movements_user
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_stock_movements_quantity_change
        CHECK (
            ABS(quantity_after - quantity_before) = quantity
        )
);

CREATE INDEX idx_inventories_variant_id
    ON inventories(variant_id);

CREATE INDEX idx_stock_movements_inventory_created
    ON stock_movements(inventory_id, created_at DESC);

CREATE INDEX idx_stock_movements_reference
    ON stock_movements(reference_type, reference_id);
