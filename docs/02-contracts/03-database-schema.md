# Vantage: Database Schema Specification

| Field | Value |
|-------|-------|
| Project | Vantage |
| Document | PostgreSQL Database Schema (Level 2) |
| Version | 1.0 |
| Date | 2026-07-23 |
| Status | Approved |

## 1. Global Schema Rules
AI agents must adhere to the following global rules when generating JPA entities and Flyway migrations:
- **Primary Keys:** All primary keys must be `UUID` mapped to `java.util.UUID` in Java. Do not use auto-incrementing integers.
- **Multi-Tenancy:** All tenant-scoped tables MUST include a `tenant_id` column of type `UUID`. This column will be targeted by Hibernate `@Filter` and must be indexed.
- **Auditing:** All tables MUST include `created_at` and `updated_at` columns of type `TIMESTAMPTZ`. These should be managed by Spring Data JPA `@EntityListeners` (AuditingEntityListener).
- **Optimistic Locking:** Tables subject to concurrent updates (e.g., `inventory`) MUST include a `version` column of type `BIGINT`, annotated with JPA `@Version`.
- **Naming Convention:** Tables and columns must use `snake_case`. Java entities and fields must use `camelCase` with `@Table` and `@Column` name overrides.

---

## 2. Core Domain Tables

### 2.1 `vendors` (Tenant Root)
Manages vendor registration and tenant lifecycle.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | Vendor/Tenant ID. |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Vendor login email. |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password. |
| `store_name` | VARCHAR(255) | NOT NULL | Display name of the store. |
| `tenant_slug` | VARCHAR(100) | UNIQUE, NOT NULL | URL-friendly unique identifier for the tenant. |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE` or `SUSPENDED`. |
| `created_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |

### 2.2 `products`
Tenant-scoped product catalog.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | Product ID. |
| `tenant_id` | UUID | NOT NULL, FK | References `vendors(id)`. |
| `name` | VARCHAR(255) | NOT NULL | Product name. |
| `price` | NUMERIC(10,2)| NOT NULL | Product price. |
| `sku` | VARCHAR(100) | NOT NULL | Stock Keeping Unit. |
| `created_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
**Indexes:** `idx_products_tenant_id` ON (`tenant_id`).

### 2.3 `inventory`
Tenant-scoped stock levels. Requires optimistic locking.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | Inventory ID. |
| `tenant_id` | UUID | NOT NULL, FK | References `vendors(id)`. |
| `product_id` | UUID | NOT NULL, FK | References `products(id)`. |
| `quantity` | INTEGER | NOT NULL | Available stock. Must be >= 0. |
| `version` | BIGINT | NOT NULL | JPA `@Version` for optimistic locking. |
| `created_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
**Indexes:** `idx_inventory_tenant_id` ON (`tenant_id`), `uq_inventory_tenant_product` UNIQUE ON (`tenant_id`, `product_id`).

### 2.4 `orders`
Tenant-scoped order records and Saga state.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | Order ID. |
| `tenant_id` | UUID | NOT NULL, FK | References `vendors(id)`. |
| `product_id` | UUID | NOT NULL, FK | References `products(id)`. |
| `quantity` | INTEGER | NOT NULL | Ordered quantity. |
| `status` | VARCHAR(20) | NOT NULL | Saga state: `CREATED`, `VALIDATED`, `PAID`, `PROCESSING`, `SHIPPED`, `CANCELLED`, `FAILED`. |
| `created_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
**Indexes:** `idx_orders_tenant_id` ON (`tenant_id`).

---

## 3. Infrastructure Tables

### 3.1 `outbox_events`
Implements the Transactional Outbox pattern. Written in the same transaction as `orders`.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | Event ID. Used for publisher confirms. |
| `tenant_id` | UUID | NOT NULL | Tenant context for the event. |
| `aggregate_type` | VARCHAR(50) | NOT NULL | e.g., `ORDER`. |
| `aggregate_id` | UUID | NOT NULL | ID of the entity the event relates to. |
| `event_type` | VARCHAR(50) | NOT NULL | e.g., `OrderCreatedEvent`. |
| `payload` | JSONB | NOT NULL | Serialized event payload (JSON). |
| `status` | VARCHAR(20) | NOT NULL | `PENDING` or `PUBLISHED`. |
| `created_at` | TIMESTAMPTZ | NOT NULL | Used for polling order. |
| `published_at` | TIMESTAMPTZ | NULL | Set when successfully ACKed by RabbitMQ. |
**Indexes:** `idx_outbox_status_created` ON (`status`, `created_at`).

### 3.2 `processed_events`
Consumer-side idempotency table. Ensures exactly-once processing of RabbitMQ events.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `event_id` | UUID | PK | The ID from `outbox_events.id`. |
| `tenant_id` | UUID | NOT NULL | Tenant context. |
| `processed_at` | TIMESTAMPTZ | NOT NULL | Time the event was consumed. |
**Indexes:** `idx_processed_events_tenant` ON (`tenant_id`).

### 3.3 `idempotency_keys`
Implements API idempotency for the payment endpoint.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `id` | UUID | PK | The `Idempotency-Key` provided by the client. |
| `tenant_id` | UUID | NOT NULL | Tenant context. |
| `request_hash` | VARCHAR(255) | NOT NULL | SHA-256 hash of the request body to detect payload tampering. |
| `response_status` | INTEGER | NOT NULL | HTTP status code of the original response. |
| `response_body` | JSONB | NOT NULL | Serialized original response payload. |
| `expires_at` | TIMESTAMPTZ | NOT NULL | TTL (24 hours from creation). |
| `created_at` | TIMESTAMPTZ | NOT NULL | Audit timestamp. |
**Indexes:** `idx_idempotency_tenant_expires` ON (`tenant_id`, `expires_at`).
