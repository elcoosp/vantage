CREATE TABLE order_search_view (
    order_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_search_view_tenant_id ON order_search_view (tenant_id);
CREATE INDEX idx_order_search_view_status ON order_search_view (status);
CREATE INDEX idx_order_search_view_tenant_status ON order_search_view (tenant_id, status);
