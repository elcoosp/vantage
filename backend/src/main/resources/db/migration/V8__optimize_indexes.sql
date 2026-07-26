CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_status_pending ON orders (tenant_id, status) WHERE status = 'CREATED';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_tenant_sku ON products (tenant_id, sku);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_tenant_product ON inventory (tenant_id, product_id);
