# TASK-050: Final Integration, E2E Smoke Test, and Demo Verification

## Product Context
- Read: `docs/04-tasks/TASK-027-readme-polish-and-demo-script.md` (The 90-second demo script)
- Read: All prior task manifests.

## Objective
Execute a comprehensive end-to-end smoke test of the entire Vantage platform. This task does not introduce new features. Instead, it verifies that all modules (Backend, Frontend, Infrastructure, Observability) integrate cohesively. It ensures the system is flawlessly prepared for the 90-second recruiter demo video and validates that the $0 hosting deployment pipeline is fully operational.

## Execution Boundaries
- You may ONLY create or modify the root `e2e-smoke-test.sh` script and the root `Makefile` (if one exists to aggregate commands).
- DO NOT modify backend source code, frontend components, or CI workflows.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/04-tasks/TASK-027-readme-polish-and-demo-script.md`

## Acceptance Criteria

### 1. Full Stack Local Verification Script
1. Create `e2e-smoke-test.sh` in the root directory.
2. The script must automate the following local verification flow:
   - Start infrastructure via `docker-compose up -d` (Postgres, RabbitMQ, Prometheus, Grafana).
   - Wait for Postgres and RabbitMQ to be healthy.
   - Start the Spring Boot backend (`./gradlew bootRun` in background).
   - Wait for the backend Actuator health endpoint to return `UP`.
   - Start the Vite frontend dev server (`npm run dev` in background).
   - Wait for the frontend to be reachable on port 5173.

### 2. API Flow Execution (The Demo Scenario)
1. The script must use `curl` to execute the exact 90-second demo scenario:
   - Register Vendor A and Vendor B.
   - Authenticate as Vendor A.
   - Create a Product and initialize Inventory to 1.
   - Trigger the Chaos Monkey (Enable Payment Failure via Admin API).
   - Place an Order for the product.
   - Poll the Order status endpoint until it returns `CANCELLED` (verifying the Saga compensation).
   - Verify Inventory is back to 1.
   - Disable the Chaos Monkey.
   - Place another Order.
   - Poll the Order status until `PAID`.
   - Verify the Grafana Prometheus endpoint has metrics (`curl http://localhost:8080/actuator/prometheus`).

### 3. Test Suite Execution
1. The script must execute the full backend test suite: `./gradlew qualityGate` (ensuring JaCoCo and PITest thresholds pass).
2. The script must execute the frontend build: `cd frontend && npm run build`.

### 4. Graceful Teardown
1. The script must trap `EXIT` and kill the background Java and Node processes.
2. The script must run `docker-compose down` to clean up infrastructure.

### 5. Verification (Output Instructions)
- Provide the command to run the smoke test: `chmod +x e2e-smoke-test.sh && ./e2e-smoke-test.sh`.
- The script should exit with a `0` status code only if every step succeeds, proving the platform is ready for recording the Loom demo video.

## Target File Paths
- `e2e-smoke-test.sh`
