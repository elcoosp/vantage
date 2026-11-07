-- V16__create_infrastructure_tables.sql
-- Creates the infrastructure tables that the code writes to on every
-- order/payment but that NO earlier migration ever created: the outbox, the
-- consumer idempotency ledger, the audit trail and the payment idempotency
-- store. On a clean database every order insert used to fail with
-- "relation entity_events does not exist" — the saga and the audit timeline
-- were vaporware without this schema.

-- ---------------------------------------------------------------------------
-- Transactional outbox (OutboxPoller relays rows to RabbitMQ)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events (status);
CREATE INDEX IF NOT EXISTS idx_outbox_events_tenant ON outbox_events (tenant_id);

-- ---------------------------------------------------------------------------
-- Consumer idempotency ledger (ProcessedEvent: one row per eventId × consumer)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID NOT NULL,
    consumer VARCHAR(128) NOT NULL,
    tenant_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_tenant ON processed_events (tenant_id);

-- ---------------------------------------------------------------------------
-- Audit trail (AuditHelper captures entity lifecycle events; powers the
-- per-order audit timeline in the orders UI)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entity_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_entity_events_aggregate ON entity_events (aggregate_id, created_at);
CREATE INDEX IF NOT EXISTS idx_entity_events_tenant ON entity_events (tenant_id);

-- ---------------------------------------------------------------------------
-- Payment idempotency keys (24h replay protection for POST /payments)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_key_tenant UNIQUE (idempotency_key, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires ON idempotency_keys (expires_at);

-- ---------------------------------------------------------------------------
-- Storefront editor configuration (one jsonb layout per tenant)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS storefront_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    layout_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_storefront_configs_tenant ON storefront_configs (tenant_id);
