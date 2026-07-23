-- V1__init.sql
-- Flyway baseline migration to prevent startup crashes when validating empty schema.
CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    tenant_slug VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
