-- Idempotent setup for full-text search using a DO block
DO $do$
BEGIN
    -- Add columns if not exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='products' AND column_name='search_vector') THEN
        ALTER TABLE products ADD COLUMN search_vector tsvector;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='search_vector') THEN
        ALTER TABLE orders ADD COLUMN search_vector tsvector;
    END IF;

    -- Drop existing triggers and functions
    DROP TRIGGER IF EXISTS products_search_vector_trigger ON products;
    DROP TRIGGER IF EXISTS orders_search_vector_trigger ON orders;
    DROP FUNCTION IF EXISTS products_search_vector_update() CASCADE;
    DROP FUNCTION IF EXISTS orders_search_vector_update() CASCADE;

    -- Create functions
    CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS trigger AS $func$
    BEGIN
      NEW.search_vector := setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
                           setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B');
      RETURN NEW;
    END;
    $func$ LANGUAGE plpgsql;

    CREATE OR REPLACE FUNCTION orders_search_vector_update() RETURNS trigger AS $func$
    BEGIN
      NEW.search_vector := setweight(to_tsvector('english', coalesce(NEW.status, '')), 'A') ||
                           setweight(to_tsvector('english', coalesce(NEW.id::text, '')), 'B');
      RETURN NEW;
    END;
    $func$ LANGUAGE plpgsql;

    -- Create triggers
    CREATE TRIGGER products_search_vector_trigger
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

    CREATE TRIGGER orders_search_vector_trigger
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION orders_search_vector_update();

    -- Create indexes
    CREATE INDEX IF NOT EXISTS idx_products_search_vector ON products USING GIN (search_vector);
    CREATE INDEX IF NOT EXISTS idx_orders_search_vector ON orders USING GIN (search_vector);
END;
$do$;
