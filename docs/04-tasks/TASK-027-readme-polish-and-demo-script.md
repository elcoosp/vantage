# TASK-027: Final Polish, README, and Demo Script

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Recall the core value proposition)
- Read: All prior task manifests to aggregate the feature set.

## Objective
Create a world-class, recruiter-ready `README.md` for the Vantage repository. This document serves as the landing page for the project. It must immediately hook the reader, display the technology stack, embed architecture diagrams, list the "Wow" features, and provide a concrete 90-second video demo script to guide the creation of the Loom video.

## Execution Boundaries
- You may ONLY create or modify the root `README.md` file.
- DO NOT modify any source code, configuration files, or other documentation.

## Context Files to Inject
- `docs/00-product/01-vision-and-personas.md`
- `docs/01-architecture/01-system-design-and-architecture.md`
- `docs/04-tasks/TASK-001-project-bootstrap.md` through `TASK-026-streaming-llm-support-chat.md` (Reference for feature list)

## Acceptance Criteria

### 1. Header and Badges
1. Project Title: **Vantage**
2. Tagline: *A production-grade, multi-tenant SaaS platform enabling independent merchants to manage operations, with distributed order orchestration and AI-driven forecasting.*
3. Include badges for: Java 21, Spring Boot 3.4, React 19, PostgreSQL, RabbitMQ, CI Status, License.

### 2. Architecture Overview (Mermaid)
1. Include a high-level Mermaid diagram showing the React Frontend -> Spring Boot API -> PostgreSQL/RabbitMQ -> Async Workers -> External Services flow.
2. Keep the diagram clean and readable.

### 3. The "Wow" Features
1. Create a bulleted list highlighting the 6 key differentiators:
   - Multi-Tenant Isolation (Hibernate `@Filter`)
   - Flash-Sale Concurrency (JPA `@Version` Optimistic Locking)
   - Distributed Saga & Outbox Pattern (At-Least-Once Delivery)
   - Resilience4j Circuit Breakers & Retries
   - OpenTelemetry End-to-End Tracing
   - Pure-Java AI Demand Forecasting (Holt-Winters)

### 4. Technology Stack
1. Create a table listing the Backend, Frontend, Infrastructure, and Observability technologies used.

### 5. Quickstart (Local Development)
1. Provide the exact commands to clone the repo, start Docker Compose (Postgres/RabbitMQ), run the backend (`./gradlew bootRun`), and run the frontend (`npm install && npm run dev`).

### 6. 90-Second Demo Script
1. Include a clearly formatted script for a 90-second Loom video demo. Break it down by timestamps (e.g., 0:00-0:15, 0:15-0:45) and describe exactly what to show on screen and what to say.
   - Scene 1: Intro & React 19 UI (Optimistic updates, Cmd+K)
   - Scene 2: Flash Sale Concurrency (3 tabs hitting buy simultaneously, 1 succeeds, 2 get 409 Conflict)
   - Scene 3: Chaos Monkey (Toggle payment failure, place order, watch Saga auto-compensate and restore inventory)
   - Scene 4: Observability (Show Grafana Tempo trace of the failed payment and compensation)
   - Scene 5: AI Forecasting (Show the Recharts graph with confidence intervals)
   - Scene 6: Live Ops Map (Show WebSocket map dropping pins globally in real-time)

### 7. Verification (Output Instructions)
- Ensure the markdown is syntactically valid and renders correctly on GitHub.
- Do not output the content of the README in the chat, only write the file.

## Target File Paths
- `README.md`
