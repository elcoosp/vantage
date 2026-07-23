# TASK-031: Implement DevSecOps Scanning (CodeQL, SonarCloud, Trivy)

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Engineering Maturity Layer)
- Read: `docs/03-meta/agent-protocol.md` (Understand quality standards)

## Objective
Integrate automated security and code quality scanning into the GitHub Actions CI pipeline. This proves to recruiters that you understand DevSecOps and shift-left security. The pipeline will run GitHub CodeQL for semantic code analysis, SonarCloud for code quality/coverage gates, and Trivy for container vulnerability scanning.

## Execution Boundaries
- You may ONLY create or modify files inside `.github/workflows/`, `.github/dependabot.yml`, `backend/build.gradle.kts` (for Sonar config), and `frontend/package.json` (for Sonar config if applicable).
- DO NOT modify application business logic.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `.github/workflows/ci.yml`

## Acceptance Criteria

### 1. Dependabot Configuration
1. Create `.github/dependabot.yml`.
2. Configure it to check for GitHub Actions dependencies weekly.
3. Configure it to check for Gradle (`backend/`) dependencies weekly.
4. Configure it to check for npm (`frontend/`) dependencies weekly.

### 2. GitHub CodeQL Analysis
1. Create `.github/workflows/codeql-analysis.yml`.
2. Trigger on `push` to `main` and `pull_request`.
3. Configure it to analyze the `java` and `javascript` languages.
4. Use the standard CodeQL action (`github/codeql-action/init@v3`, `autobuild`, `analyze`).

### 3. SonarCloud Integration
1. Create `.github/workflows/sonarcloud.yml`.
2. Trigger on `push` to `main` and `pull_request`.
3. Use the SonarSource action (`SonarSource/sonarcloud-github-action@master`).
4. Configure `SONAR_TOKEN` as a repository secret.
5. For the backend (`backend/build.gradle.kts`), apply the `org.sonarqube` plugin.
6. Configure `sonar.projectKey`, `sonar.organization`, and point `sonar.host.url` to `https://sonarcloud.io`.
7. Ensure JaCoCo XML reports are generated so Sonar can parse coverage.

### 4. Trivy Container Scanning
1. Update the `.github/workflows/cd.yml` (or `ci.yml`) to include a Trivy scan step after the backend Docker image is built.
2. Use `aquasecurity/trivy-action@master`.
3. Scan the built Docker image for `HIGH` and `CRITICAL` vulnerabilities.
4. Fail the pipeline if critical vulnerabilities are found (exit code 1).

### 5. Verification (Output Instructions)
- Document the required GitHub repository secrets (`SONAR_TOKEN`).
- Confirm that the README will display badges for CodeQL and SonarCloud quality gate.

## Target File Paths
- `.github/dependabot.yml`
- `.github/workflows/codeql-analysis.yml`
- `.github/workflows/sonarcloud.yml`
- `.github/workflows/cd.yml` (Modify to add Trivy step)
- `backend/build.gradle.kts` (Modify to add Sonar plugin)
