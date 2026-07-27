# Vantage: 8-Agent Parallel Execution Plan

This document defines the exact execution sequence to build Vantage using up to 8 AI agents in parallel with ZERO merge conflicts.

## 1. Golden Rules for Zero Conflicts

1. **Branch Naming:** Agents must work on branches named `agent-X/<TASK-ID>`.
2. **Build File Lock:** Only Agent 1 is allowed to modify `build.gradle.kts` or `package.json` during Phase 1 & 2. In Phase 3, Agent 8 (DevOps) owns all infrastructure files. Feature agents must NOT add dependencies; they must use what exists or request it via a code comment.
3. **Database Migrations:** Agents must use specific Flyway version prefixes assigned in the task manifest (e.g., `V10__`, `V11__`) to avoid migration conflicts.
4. **Package Isolation:** Agents must strictly respect Spring Modulith boundaries. If two agents need to touch the same package (e.g., `order`), those tasks must be serialized.
5. **Merge Protocol:** Agents must merge their PRs into `main` before the next Phase begins. Pull `main` frequently.

---

## Phase 1: The Monolith Foundation (Serial)
*Everything depends on the base scaffolding. This must be done sequentially by a single agent.*

- **Agent 1 (Backend Lead):**
  - [x] `TASK-001`: Bootstrap Monorepo & Core Infrastructure
  - [x] Merge to `main`
  - [x] `TASK-002`: Multi-Tenant Security & Tenant Isolation
  - [x] Merge to `main`
  - [x] `TASK-003`: Product Catalog & Optimistic Inventory
  - [x] Merge to `main`
  - [x] `TASK-004`: Transactional Order Creation (Outbox)
  - [x] Merge to `main`

---

## Phase 2: The Saga Core (Serial)
*The distributed transaction flow is highly coupled. Must be sequential.*

- **Agent 1 (Backend Lead):**
  - [x] `TASK-005`: Inventory Reservation Consumer & Idempotency
  - [x] Merge to `main`
  - [x] `TASK-006`: Payment Processing & Saga Compensation
  - [x] Merge to `main`
  - [x] `TASK-007`: Inventory Compensation Consumer
  - [x] Merge to `main`
  - [x] `TASK-008`: Idempotent Payment API
  - [x] Merge to `main`

---

## Phase 3: 8-Agent Parallel Sprint
*Now that the core backend and API contracts exist, we can parallelize heavily. All agents pull `main` before starting.*

- **Agent 1 (Backend Infra):**
  - [x] `TASK-013`: OpenTelemetry Distributed Tracing (Touches `core/observability`, `application.yml`)
  - [x] `TASK-047`: HikariCP Virtual Thread Optimization (Touches `application.yml`, `core/config`)
  - [x] Merge to `main`

- **Agent 2 (Backend Features - Integration):**
  - [x] `TASK-009`: HMAC-Signed Webhooks (Touches `integration/`, `vendor/`)
  - [x] `TASK-020`: Developer Portal & API Keys (Touches `integration/`, `core/security/`)
  - [x] `TASK-035`: Multi-Tenant API Rate Limiting (Touches `core/ratelimiter/`, `core/config/`)
  - [x] Merge to `main`

- **Agent 3 (Backend Features - Analytics/Search):**
  - [x] `TASK-010`: Pure-Java AI Demand Forecasting (Touches `analytics/`, Migration `V10__`)
  - [x] `TASK-019`: PostgreSQL Full-Text Search (Touches `core/search/`, `product/`, `order/`, Migration `V11__`)
  - [x] `TASK-021`: CQRS Order Search Read Model (Touches `order/query/`, Migration `V12__`)
  - [x] Merge to `main`

- **Agent 4 (Backend Features - Advanced Ops):**
  - [x] `TASK-017`: Event-Sourced Audit Timeline (Touches `core/audit/`, `order/`)
  - [x] `TASK-023`: Admin Dashboard & Chaos Monkey (Touches `core/admin/`, `payment/`)
  - [x] `TASK-029`: Bulk Product Upload Virtual Threads (Touches `product/`)
  - [x] Merge to `main`

- **Agent 5 (Backend Features - Hardening):**
  - [x] `TASK-040`: RFC 7807 Problem Details (Touches `core/exception/`)
  - [x] `TASK-034`: Event-Driven Cache Invalidation (Touches `core/cache/`, `product/`, `analytics/`)
  - [x] `TASK-038`: Database Indexing & N+1 Elimination (Touches repositories, Migration `V13__`)
  - [x] Merge to `main`

- **Agent 6 (Backend Features - Scaling):**
  - [x] `TASK-039`: Database Read Replica Routing (Touches `core/db/`, `application.yml`)
  - [x] `TASK-049`: Distributed Scheduling Postgres Locks (Touches `core/db/`, `core/messaging/`)
  - [x] `TASK-048`: GraphQL API for Order Search (Touches `core/graphql/`, `order/query/`)
  - [x] Merge to `main`

- **Agent 7 (Frontend Lead):**
  - [x] `TASK-016`: Frontend Authentication Flow
  - [x] `TASK-012`: React Dashboard & Command Palette
  - [x] `TASK-022`: Virtualized Orders Table
  - [x] `TASK-014`: AI Forecast Visualization
  - [x] `TASK-015`: Live Ops Map Frontend
  - [x] Merge to `main`

- **Agent 8 (DevOps & QA Lead):**
  - [x] `TASK-018`: CI Pipeline & Mutation Testing (Touches `.github/`, `build.gradle.kts`)
  - [x] `TASK-028`: Semantic Versioning & Release Please (Touches `.github/`)
  - [x] `TASK-031`: DevSecOps Scanning (Touches `.github/`)
  - [x] `TASK-045`: Prometheus Metrics & Grafana (Touches `grafana/`, `core/metrics/`)
  - [x] Merge to `main`

---

## Phase 4: Advanced Parallel Sprint
*Agents pull the updated `main` containing Phase 3 merges.*

- **Agent 1 (Testing & Architecture):**
  - [x] `TASK-033`: Spring Modulith Verification Tests
  - [x] `TASK-046`: ArchUnit Architectural Rule Enforcement
  - [x] `TASK-036`: Property-Based Testing for Forecasting
  - [x] `TASK-037`: Backend Performance Testing (k6)
  - [x] Merge to `main`

- **Agent 2 (Backend Polish):**
  - [ ] `TASK-030`: Resilience4j Bulkhead & Rate Limiter (Touches `payment/`, `integration/`)
  - [ ] `TASK-043`: Admin Tenant Management UI & Suspension Logic (Touches `core/admin/`, `vendor/`, `core/security/`)
  - [ ] `TASK-044`: OpenAPI Codegen & Type Sync (Touches `build.gradle.kts`, `frontend/package.json`)
  - [ ] Merge to `main`

- **Agent 3 (Frontend Polish):**
  - [ ] `TASK-025`: Developer Portal Frontend
  - [ ] `TASK-032`: Frontend Audit Timeline UI
  - [ ] `TASK-026`: Streaming LLM Support Chat
  - [ ] `TASK-041`: Server-Driven UI Storefront
  - [ ] Merge to `main`

- **Agent 8 (DevOps):**
  - [ ] `TASK-024`: $0 Hosting Deployment Pipeline
  - [ ] Merge to `main`

---

## Phase 5: Final Integration (Serial)
*Final wiring and verification.*

- **Agent 1:**
  - [ ] `TASK-027`: Final Polish, README, and Demo Script
  - [ ] `TASK-042`: End-to-End (E2E) Testing with Playwright
  - [ ] `TASK-050`: Final Integration & E2E Smoke Test
  - [ ] Merge to `main`
