-- V5__add_fts_indexes.sql
-- Drop existing columns and indexes if they exist (for idempotency)
DROP INDEX IF EXISTS idx_products_search_vector;
DROP INDEX IF EXISTS idx_orders_search_vector;

ALTER TABLE products DROP COLUMN IF EXISTS search_vector;
ALTER TABLE orders DROP COLUMN IF EXISTS search_vector;

-- Add generated tsvector columns
ALTER TABLE products
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(description, '')), 'B')
) STORED;

ALTER TABLE orders
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(status, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(id::text, '')), 'B')
) STORED;

-- Create GIN indexes
CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);
CREATE INDEX idx_orders_search_vector ON orders USING GIN (search_vector);
