-- V9__fix_api_keys_fk.sql
-- The api_keys.tenant_id FK incorrectly references vendors.id instead of vendors.tenant_id.
-- The application stores the tenant UUID in api_keys.tenant_id, which matches vendors.tenant_id.
-- Fix: drop the wrong FK and add the correct one.

ALTER TABLE api_keys DROP CONSTRAINT IF EXISTS api_keys_tenant_id_fkey;
ALTER TABLE api_keys ADD CONSTRAINT api_keys_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES vendors(tenant_id);
