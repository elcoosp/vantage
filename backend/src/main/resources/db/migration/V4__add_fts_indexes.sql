-- V4__add_fts_indexes.sql
-- Add generated tsvector columns for full-text search on products and orders

ALTER TABLE products
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))) STORED;

CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);

ALTER TABLE orders
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (to_tsvector('english', coalesce(status, '') || ' ' || coalesce(id::text, ''))) STORED;

CREATE INDEX idx_orders_search_vector ON orders USING GIN (search_vector);
