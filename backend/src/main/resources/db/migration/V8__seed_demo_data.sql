-- V8__seed_demo_data.sql
-- Seeds realistic demo data so the app is screenshotable at a glance.
-- All entities belong to tenant 11111111-1111-1111-1111-111111111111.

-- ── Products ──────────────────────────────────────────────
INSERT INTO products (id, tenant_id, name, description, price, sku) VALUES
    ('a1b2c3d4-1111-4aaa-8bbb-111111111111', '11111111-1111-1111-1111-111111111111', 'Premium Wireless Headphones', 'Noise-cancelling over-ear Bluetooth headphones with 40h battery life', 249.99, 'AUDIO-PREM-001'),
    ('a1b2c3d4-2222-4bbb-8ccc-222222222222', '11111111-1111-1111-1111-111111111111', 'Mechanical Keyboard Pro', 'Hot-swappable RGB mechanical keyboard with Cherry MX switches', 129.50, 'KEYBOARD-PRO-002'),
    ('a1b2c3d4-3333-4ccc-9ddd-333333333333', '11111111-1111-1111-1111-111111111111', 'Ultra HD 4K Webcam', 'Professional 4K webcam with autofocus and dual microphones', 199.00, 'CAMERA-UHD-003')
ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name, description = EXCLUDED.description, price = EXCLUDED.price;

-- ── Inventory ─────────────────────────────────────────────
INSERT INTO inventory (id, tenant_id, product_id, quantity, version) VALUES
    ('b1c2d3e4-1111-4aaa-9bbb-111111111111', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-1111-4aaa-8bbb-111111111111', 42, 0),
    ('b1c2d3e4-2222-4bbb-0ccc-222222222222', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-2222-4bbb-8ccc-222222222222', 15, 0),
    ('b1c2d3e4-3333-4ccc-1ddd-333333333333', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-3333-4ccc-9ddd-333333333333', 8, 0)
ON CONFLICT (product_id) DO UPDATE
    SET quantity = EXCLUDED.quantity;

-- ── Orders ────────────────────────────────────────────────
-- Recent orders across all statuses
INSERT INTO orders (id, tenant_id, product_id, quantity, status) VALUES
    ('c1d2e3f4-1111-4aaa-9bbb-111111111111', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-1111-4aaa-8bbb-111111111111', 2, 'DELIVERED'),
    ('c1d2e3f4-2222-4bbb-0ccc-222222222222', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-2222-4bbb-8ccc-222222222222', 1, 'DELIVERED'),
    ('c1d2e3f4-3333-4ccc-1ddd-333333333333', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-1111-4aaa-8bbb-111111111111', 1, 'SHIPPED'),
    ('c1d2e3f4-4444-4ddd-2eee-444444444444', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-3333-4ccc-9ddd-333333333333', 3, 'PAID'),
    ('c1d2e3f4-5555-4eee-3fff-555555555555', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-2222-4bbb-8ccc-222222222222', 1, 'CONFIRMED'),
    ('c1d2e3f4-6666-4fff-4aaa-666666666666', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-1111-4aaa-8bbb-111111111111', 2, 'CREATED'),
    ('c1d2e3f4-7777-4aaa-5bbb-777777777777', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-3333-4ccc-9ddd-333333333333', 1, 'CANCELLED'),
    ('c1d2e3f4-8888-4bbb-6ccc-888888888888', '11111111-1111-1111-1111-111111111111', 'a1b2c3d4-2222-4bbb-8ccc-222222222222', 1, 'PAID')
ON CONFLICT (id) DO NOTHING;

-- Spread order timestamps across the last 7 days so the chart shows activity
UPDATE orders SET created_at = NOW() - INTERVAL '1 day' WHERE id = 'c1d2e3f4-1111-4aaa-9bbb-111111111111';
UPDATE orders SET created_at = NOW() - INTERVAL '1 day'   WHERE id = 'c1d2e3f4-2222-4bbb-0ccc-222222222222';
UPDATE orders SET created_at = NOW() - INTERVAL '2 days'   WHERE id = 'c1d2e3f4-3333-4ccc-1ddd-333333333333';
UPDATE orders SET created_at = NOW() - INTERVAL '2 days'   WHERE id = 'c1d2e3f4-4444-4ddd-2eee-444444444444';
UPDATE orders SET created_at = NOW() - INTERVAL '3 days'   WHERE id = 'c1d2e3f4-5555-4eee-3fff-555555555555';
UPDATE orders SET created_at = NOW() - INTERVAL '4 days'  WHERE id = 'c1d2e3f4-6666-4fff-4aaa-666666666666';
UPDATE orders SET created_at = NOW() - INTERVAL '5 days'  WHERE id = 'c1d2e3f4-7777-4aaa-5bbb-777777777777';
UPDATE orders SET created_at = NOW() - INTERVAL '6 days'  WHERE id = 'c1d2e3f4-8888-4bbb-6ccc-888888888888';

-- Populate order_search_view so the search API returns results immediately
INSERT INTO order_search_view (order_id, tenant_id, product_name, status, quantity, created_at) VALUES
    ('c1d2e3f4-1111-4aaa-9bbb-111111111111', '11111111-1111-1111-1111-111111111111', 'Premium Wireless Headphones', 'DELIVERED', 2, NOW() - INTERVAL '1 day'),
    ('c1d2e3f4-2222-4bbb-0ccc-222222222222', '11111111-1111-1111-1111-111111111111', 'Mechanical Keyboard Pro', 'DELIVERED', 1, NOW() - INTERVAL '1 day'),
    ('c1d2e3f4-3333-4ccc-1ddd-333333333333', '11111111-1111-1111-1111-111111111111', 'Premium Wireless Headphones', 'SHIPPED', 1, NOW() - INTERVAL '2 days'),
    ('c1d2e3f4-4444-4ddd-2eee-444444444444', '11111111-1111-1111-1111-111111111111', 'Ultra HD 4K Webcam', 'PAID', 3, NOW() - INTERVAL '2 days'),
    ('c1d2e3f4-5555-4eee-3fff-555555555555', '11111111-1111-1111-1111-111111111111', 'Mechanical Keyboard Pro', 'CONFIRMED', 1, NOW() - INTERVAL '3 days'),
    ('c1d2e3f4-6666-4fff-4aaa-666666666666', '11111111-1111-1111-1111-111111111111', 'Premium Wireless Headphones', 'CREATED', 2, NOW() - INTERVAL '4 days'),
    ('c1d2e3f4-7777-4aaa-5bbb-777777777777', '11111111-1111-1111-1111-111111111111', 'Ultra HD 4K Webcam', 'CANCELLED', 1, NOW() - INTERVAL '5 days'),
    ('c1d2e3f4-8888-4bbb-6ccc-888888888888', '11111111-1111-1111-1111-111111111111', 'Mechanical Keyboard Pro', 'PAID', 1, NOW() - INTERVAL '6 days')
ON CONFLICT (order_id) DO UPDATE
    SET product_name = EXCLUDED.product_name, status = EXCLUDED.status, quantity = EXCLUDED.quantity, created_at = EXCLUDED.created_at;
