# TASK-024: Implement $0 Hosting Deployment Pipeline (Render, Neon, Vercel)

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 4. Container View)
- Read: `docs/00-product/01-vision-and-personas.md` (Understand the budget constraint for a portfolio startup)

## Objective
Configure the deployment infrastructure to host the full-stack Vantage platform for $0. This involves containerizing the Spring Boot backend, setting up a GitHub Actions CD pipeline to deploy to Render, migrating the database to Neon (PostgreSQL), migrating the message broker to CloudAMQP, and deploying the React frontend to Vercel.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/Dockerfile`, `.github/workflows/`, `frontend/vercel.json`, and `backend/src/main/resources/application.yml` (for environment variable overrides).
- DO NOT modify Java source code or React component logic.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/build.gradle.kts`
- `frontend/package.json`

## Acceptance Criteria

### 1. Backend Containerization
1. Create a multi-stage `Dockerfile` in the `backend/` directory.
2. Stage 1 (Builder): Use `gradle:8.7-jdk21` to compile the application and extract layers.
3. Stage 2 (Runner): Use `eclipse-temurin:21-jre-alpine` for a small attack surface.
4. Expose port `8080`.
5. Configure the entrypoint to run the Spring Boot application with Virtual Threads enabled (`-Dspring.threads.virtual.enabled=true`).

### 2. Environment Configuration
1. Update `backend/src/main/resources/application.yml` to use environment variables for all production secrets and external service URLs.
   - `SPRING_DATASOURCE_URL` (Neon PostgreSQL)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_RABBITMQ_HOST` (CloudAMQP)
   - `SPRING_RABBITMQ_PORT`
   - `SPRING_RABBITMQ_USERNAME`
   - `SPRING_RABBITMQ_PASSWORD`
   - `SPRING_RABBITMQ_VIRTUAL_HOST`
   - `OTEL_EXPORTER_OTLP_ENDPOINT` (Grafana Cloud)
   - `JWT_SECRET`
2. Ensure Flyway runs automatically on startup (`spring.flyway.enabled=true`).

### 3. GitHub Actions CD Pipeline
1. Create `.github/workflows/cd.yml`.
2. Trigger on `push` to `main` (after the CI workflow from Task-018 passes).
3. Backend Deployment Job:
   - Trigger a Render Deploy Hook via `curl` or use the `render-oss/render-action@v1`.
4. Frontend Deployment Job:
   - Trigger a Vercel deployment using the `amondnet/vercel-action@v25` or official Vercel GitHub integration.
   - Pass the backend URL as an environment variable to the frontend build.

### 4. Frontend SPA Configuration
1. Create `frontend/vercel.json`.
2. Configure routing to rewrite all paths to `/index.html` for React Router to handle.
3. Ensure the production build uses the Render backend URL as the API base URL.

### 5. Verification (Output Instructions)
- Document the steps required to provision the free tiers on Render, Neon, CloudAMQP, and Vercel.
- Document which GitHub Repository Secrets need to be configured for the CD pipeline to function.

## Target File Paths
- `backend/Dockerfile`
- `.github/workflows/cd.yml`
- `frontend/vercel.json`
- `backend/src/main/resources/application.yml`
