-- V1__init.sql
-- Complete schema for vendors and related tables

CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    webhook_url VARCHAR(255),
    webhook_secret VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendors_tenant_id ON vendors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_vendors_email ON vendors(email);

-- Seed admin tenant
INSERT INTO vendors (id, tenant_id, email, password_hash, company_name, status, is_admin, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'admin@vantage.com',
    '$2a$10$dummyhashforadmin',
    'Vantage Admin',
    'ACTIVE',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (tenant_id) DO NOTHING;
