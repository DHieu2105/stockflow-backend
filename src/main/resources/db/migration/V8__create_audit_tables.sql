
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,

    changed_by UUID,

    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_status_history_user
        FOREIGN KEY (changed_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_order_history_old_status
        CHECK (
            old_status IS NULL OR old_status IN (
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

    CONSTRAINT chk_order_history_new_status
        CHECK (
            new_status IN (
                'PENDING_PAYMENT',
                'CONFIRMED',
                'PROCESSING',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED',
                'DELIVERY_FAILED',
                'RETURNED'
            )
        )
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    actor_id UUID,

    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,

    old_values JSONB,
    new_values JSONB,

    ip_address INET,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_audit_logs_old_values
        CHECK (
            old_values IS NULL OR jsonb_typeof(old_values) = 'object'
        ),

    CONSTRAINT chk_audit_logs_new_values
        CHECK (
            new_values IS NULL OR jsonb_typeof(new_values) = 'object'
        )
);

CREATE INDEX idx_order_status_history_order_created
    ON order_status_history(order_id, created_at DESC);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_audit_logs_actor_created
    ON audit_logs(actor_id, created_at DESC);
