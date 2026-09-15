-- V15__seed_ai_documents.sql
-- Seeds help docs and product descriptions into ai_documents for RAG.
-- Scoped to the demo admin tenant.

INSERT INTO ai_documents (tenant_id, title, content, source_url, content_hash) VALUES
('11111111-1111-1111-1111-111111111111', 'Vantage Help: Orders',
'Orders in Vantage flow through a lifecycle: CREATED → CONFIRMED → PAID → SHIPPED → DELIVERED. If an order is cancelled, it goes to CANCELLED and triggers a compensating inventory release. You can check order status by asking the support assistant about a specific order ID.',
'/help/orders', 'a1b2c3d4-orders-help'),
('11111111-1111-1111-1111-111111111111', 'Vantage Help: Inventory',
'Inventory levels are tracked per product and per tenant. Low-stock items are flagged on the dashboard. Use the support assistant to check current stock for any product by name or ID. Optimistic concurrency (version headers) prevents overselling during flash sales.',
'/help/inventory', 'a1b2c3d4-inventory-help'),
('11111111-1111-1111-1111-111111111111', 'Vantage Help: Demand Forecasting',
'Vantage uses Holt-Winters exponential smoothing to forecast demand for the next 7 days based on 30 days of sales history. The forecast includes 95% confidence intervals. Higher sales on weekends indicate weekly seasonality, which the model accounts for.',
'/help/forecasting', 'a1b2c3d4-forecast-help'),
('11111111-1111-1111-1111-111111111111', 'Vantage Help: Products',
'Products are multi-tenant scoped. Each product has a name, SKU, description, and price. You can search for products by name or SKU using the support assistant. Product IDs are UUIDs.',
'/help/products', 'a1b2c3d4-products-help')
ON CONFLICT (content_hash) DO NOTHING;
