-- Idempotent setup for full-text search using IF EXISTS/IF NOT EXISTS

ALTER TABLE products ADD COLUMN IF NOT EXISTS search_vector tsvector;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS search_vector tsvector;

DROP TRIGGER IF EXISTS products_search_vector_trigger ON products;
DROP TRIGGER IF EXISTS orders_search_vector_trigger ON orders;

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

CREATE TRIGGER products_search_vector_trigger
BEFORE INSERT OR UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

CREATE TRIGGER orders_search_vector_trigger
BEFORE INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION orders_search_vector_update();

CREATE INDEX IF NOT EXISTS idx_products_search_vector ON products USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS idx_orders_search_vector ON orders USING GIN (search_vector);
