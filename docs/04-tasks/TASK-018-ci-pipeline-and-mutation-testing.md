# TASK-018: Implement CI Pipeline, JaCoCo, and PITest Mutation Testing

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand engineering maturity requirements)
- Read: `docs/03-meta/agent-protocol.md` (Section 5. Testing Requirements)

## Objective
Establish a production-grade CI/CD pipeline using GitHub Actions. Configure JaCoCo for line/branch coverage enforcement (>= 80%) and PITest for mutation testing enforcement (>= 70% mutation score). This ensures the codebase is resilient to false-positive tests and maintains high quality gates before merging.

## Execution Boundaries
- You may ONLY create or modify files inside `.github/workflows/`, `backend/build.gradle.kts`, and `backend/src/test/`.
- DO NOT modify `application.yml`, main source code in `backend/src/main/`, or any files in `frontend/`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/build.gradle.kts`

## Acceptance Criteria

### 1. Gradle Configuration (JaCoCo & PITest)
1. Update `backend/build.gradle.kts` to apply the `jacoco` and `info.solidsoft.pitest` plugins.
2. Configure JaCoCo:
   - `jacocoTestReport` must depend on `test`.
   - `jacocoTestCoverageVerification` must enforce: Minimum 80% line coverage, 80% branch coverage.
   - Exclude domain entities, DTOs, and configuration classes from the report (e.g., `**/domain/**`, `**/dto/**`, `**/config/**`, `**/VantageApplication.java`).
3. Configure PITest:
   - Target classes: `com.vantage.**`.
   - Target tests: `com.vantage.**`.
   - Output formats: `HTML`, `XML`.
   - Mutation threshold: 70%.
   - History input/output file for incremental analysis.
4. Create a custom Gradle task `qualityGate` that depends on `build`, `jacocoTestCoverageVerification`, and `pitest`.

### 2. GitHub Actions CI Pipeline
1. Create `.github/workflows/ci.yml`.
2. Trigger on `push` to `main` and `pull_request`.
3. Define a matrix strategy for JDK 21 (and optionally 22 to prove forward compatibility).
4. Steps:
   - Checkout code.
   - Setup JDK.
   - Setup Node.js for frontend.
   - Cache Gradle and npm dependencies.
   - Run `./gradlew qualityGate` in `backend/`.
   - Run `npm ci && npm run build` in `frontend/`.
   - Upload JaCoCo and PITest HTML reports as CI artifacts.

### 3. Verification (Output Instructions)
- Provide the terminal command to run the quality gate locally: `./gradlew qualityGate`.
- Confirm that the build fails if mutation score drops below 70% or coverage drops below 80%.

## Target File Paths
- `.github/workflows/ci.yml`
- `backend/build.gradle.kts`
