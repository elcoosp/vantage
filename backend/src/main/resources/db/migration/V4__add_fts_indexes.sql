-- V4__add_fts_indexes.sql
-- Add generated tsvector columns for full-text search on products and orders

ALTER TABLE products
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(description, '')), 'B')
) STORED;

CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);

ALTER TABLE orders
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(status, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(id::text, '')), 'B')
) STORED;

CREATE INDEX idx_orders_search_vector ON orders USING GIN (search_vector);
