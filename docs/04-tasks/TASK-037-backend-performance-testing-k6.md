# TASK-037: Implement Backend Performance Testing (k6)

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Engineering Maturity)
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Flash-Sale Concurrency)

## Objective
Implement automated performance and load testing using k6 to verify the system meets its non-functional requirements (NFRs) under stress. The tests will simulate a flash-sale scenario (1,000 concurrent users hitting the order placement endpoint) and assert that the P95 latency remains under 200ms and the error rate is 0% (excluding expected 409 conflicts).

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/test/performance/` and `.github/workflows/perf.yml`.
- DO NOT modify application business logic or `application.yml`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `POST /api/v1/orders`, `POST /api/v1/vendors/register`)

## Acceptance Criteria

### 1. k6 Test Script
1. Create `backend/src/test/performance/flash_sale.js`.
2. Define test stages:
   - Warmup: Ramp up to 50 users over 30 seconds.
   - Peak Load: Ramp up to 1,000 users over 1 minute.
   - Sustain: Hold at 1,000 users for 2 minutes.
   - Ramp down: 30 seconds.
3. Logic:
   - Each iteration should authenticate as a pre-seeded vendor.
   - Fetch the product catalog.
   - Place an order for a specific product.
4. Define thresholds:
   - `http_req_duration`: p(95) < 200ms.
   - `http_req_failed`: rate < 0.01 (excluding 409 Conflict).

### 2. GitHub Actions Workflow
1. Create `.github/workflows/perf.yml`.
2. Trigger manually (`workflow_dispatch`) to avoid running on every PR.
3. Steps:
   - Checkout code.
   - Setup Node.js and install k6.
   - Start the backend and database via Docker Compose.
   - Wait for health checks to pass.
   - Run a setup script to seed the vendor and product data.
   - Execute `k6 run backend/src/test/performance/flash_sale.js`.
   - Upload the k6 summary as an artifact.

### 3. Verification (Output Instructions)
- Provide the local command to run the k6 test against a running backend: `k6 run backend/src/test/performance/flash_sale.js`.
- Document that the test will fail if P95 latency exceeds 200ms or error rate exceeds 1%.

## Target File Paths
- `backend/src/test/performance/flash_sale.js`
- `.github/workflows/perf.yml`
