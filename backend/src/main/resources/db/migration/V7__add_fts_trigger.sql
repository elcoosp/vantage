-- V7__add_fts_trigger.sql
-- Drop existing triggers, functions, indexes, and columns in correct order

DROP TRIGGER IF EXISTS products_search_vector_trigger ON products;
DROP TRIGGER IF EXISTS orders_search_vector_trigger ON orders;

DROP FUNCTION IF EXISTS products_search_vector_update() CASCADE;
DROP FUNCTION IF EXISTS orders_search_vector_update() CASCADE;

DROP INDEX IF EXISTS idx_products_search_vector;
DROP INDEX IF EXISTS idx_orders_search_vector;

ALTER TABLE products DROP COLUMN IF EXISTS search_vector;
ALTER TABLE orders DROP COLUMN IF EXISTS search_vector;

-- Add regular tsvector columns
ALTER TABLE products ADD COLUMN search_vector tsvector;
ALTER TABLE orders ADD COLUMN search_vector tsvector;

-- Create trigger functions
CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector := setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
                       setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION orders_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector := setweight(to_tsvector('english', coalesce(NEW.status, '')), 'A') ||
                       setweight(to_tsvector('english', coalesce(NEW.id::text, '')), 'B');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers
CREATE TRIGGER products_search_vector_trigger
BEFORE INSERT OR UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

CREATE TRIGGER orders_search_vector_trigger
BEFORE INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION orders_search_vector_update();

-- Create GIN indexes
CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);
CREATE INDEX idx_orders_search_vector ON orders USING GIN (search_vector);
