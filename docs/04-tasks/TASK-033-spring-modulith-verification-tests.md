# TASK-033: Implement Spring Modulith Boundary Verification Tests

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 5. Module Boundaries)
- Read: `docs/01-architecture/02-architecture-decision-records.md` (ADR-001: Adopt Modular Monolith)

## Objective
Enforce the architectural boundaries of the Spring Modulith structure. Write verification tests using Spring Modulith's `ApplicationModules` API to guarantee that modules (e.g., `inventory`, `order`, `payment`) do not illicitly access each other's internal repositories or entities. This prevents the modular monolith from decaying into a tangled "big ball of mud" and proves senior-level architectural discipline.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/test/java/com/vantage/`.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any main source code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/01-architecture/01-system-design-and-architecture.md`
- `backend/src/main/java/com/vantage/VantageApplication.java`

## Acceptance Criteria

### 1. Modulith Verification Test
1. Create `ModulithVerificationIT` in `backend/src/test/java/com/vantage/`.
2. Use the `ApplicationModules.of(VantageApplication.class).verify()` API provided by Spring Modulith.
3. This test will automatically fail if any module crosses its defined boundary (e.g., if `order` tries to autowire `InventoryRepository` instead of using Application Events).
4. Ensure the test is annotated with `@SpringBootTest` or runs as a standard JUnit test depending on the Spring Modulith version requirements.

### 2. Documentation Generation (Optional but recommended)
1. Add a step in the test (or a separate test) to output the module documentation to a target directory using `ApplicationModules.of(VantageApplication.class).document(Documenter::writeDocumentationAsPlantUml)`.
2. This generates PlantUML diagrams of the actual module dependencies, which can be committed to the repo as living architecture diagrams.

### 3. Verification (Output Instructions)
- Provide the terminal command to run the verification test: `./gradlew test --tests "*ModulithVerificationIT"`.
- If the test fails due to existing boundary violations, do not modify the main code. Instead, output a report of the violations so they can be addressed in a separate refactor task.

## Target File Paths
- `backend/src/test/java/com/vantage/ModulithVerificationIT.java`
