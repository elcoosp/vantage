# TASK-044: Implement OpenAPI Code Generation & Frontend Type Sync

## Product Context
- Read: `docs/02-contracts/02-rest-api-spec.yaml` (The source of truth for API contracts)
- Read: `docs/03-meta/agent-protocol.md` (Ensure frontend/backend alignment)

## Objective
Implement an automated code generation pipeline to eliminate manual DTO creation and ensure the React frontend and Spring Boot backend never drift out of sync. Use the OpenAPI Generator to generate Java records and Spring Web server interfaces on the backend, and TypeScript types/Axios clients on the frontend. This demonstrates elite-level, contract-first full-stack engineering maturity.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/build.gradle.kts` (to add OpenAPI plugin), `frontend/package.json` (to add codegen script), `frontend/src/api/generated/` (output directory), and `.github/workflows/ci.yml` (to enforce sync).
- DO NOT modify the `docs/02-contracts/02-rest-api-spec.yaml` (it is read-only here).

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml`
- `backend/build.gradle.kts`
- `frontend/package.json`

## Acceptance Criteria

### 1. Backend OpenAPI Generation
1. Add the `org.openapi.generator` plugin to `backend/build.gradle.kts`.
2. Configure a `generateOpenApiModels` task that reads `docs/02-contracts/02-rest-api-spec.yaml`.
3. Generate Java Records for all schemas (e.g., `ProductResponse`, `OrderRequest`) into `backend/src/generated/java/com/vantage/api/model`.
4. Generate Spring Web server interfaces (e.g., `ProductApi`) into `backend/src/generated/java/com/vantage/api/api`.
5. Implement the generated interfaces in the existing controllers (e.g., `ProductController implements ProductApi`).

### 2. Frontend TypeScript Generation
1. Add `openapi-typescript-codegen` to `frontend/package.json`.
2. Add a script `"generate:api": "openapi --input ../docs/02-contracts/02-rest-api-spec.yaml --output src/api/generated --client axios"`.
3. Generate the TypeScript types and Axios service classes into `frontend/src/api/generated/`.
4. Refactor existing frontend API calls (e.g., in `useOrders.ts`) to use the generated `OrderService` and types instead of manual interfaces.

### 3. CI Sync Enforcement
1. Add a step in `.github/workflows/ci.yml` to run `./gradlew generateOpenApiModels` and `npm run generate:api`.
2. Run `git diff --exit-code` to ensure no manual changes have drifted from the spec. If the generated files differ from what's committed, fail the CI pipeline.

### 4. Verification (Output Instructions)
- Provide the commands to run the codegen locally: `./gradlew generateOpenApiModels` and `cd frontend && npm run generate:api`.

## Target File Paths
- `backend/build.gradle.kts` (Modify)
- `frontend/package.json` (Modify)
- `frontend/src/api/generated/` (Generated output)
- `frontend/src/features/orders/useOrders.ts` (Modify to use generated types)
- `.github/workflows/ci.yml` (Modify)
