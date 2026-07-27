ALTER TABLE vendors ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE vendors ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Seed an admin vendor for testing (tenant_id = '11111111-1111-1111-1111-111111111111')
INSERT INTO vendors (id, tenant_id, email, password_hash, company_name, status, is_admin, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'admin@vantage.com',
    '$2a$10$dummyhashforadmin', -- dummy, but we'll override in test
    'Vantage Admin',
    'ACTIVE',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (tenant_id) DO NOTHING;
