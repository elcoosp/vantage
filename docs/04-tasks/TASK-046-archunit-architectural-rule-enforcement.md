# TASK-046: Implement ArchUnit for Granular Architectural Rule Enforcement

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 5. Module Boundaries)
- Read: `docs/03-meta/agent-protocol.md` (Section 3. Architectural Rules)

## Objective
Complement Spring Modulith verification (Task-033) with ArchUnit to enforce granular, low-level architectural rules. This prevents common architectural anti-patterns (e.g., Controllers directly accessing Repositories, Services leaking JPA entities to the UI layer) and proves elite-level commitment to code quality and maintainability.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/test/java/com/vantage/` and `backend/build.gradle.kts` (to add the ArchUnit dependency).
- DO NOT modify main source code or configuration files.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/build.gradle.kts`

## Acceptance Criteria

### 1. Dependency Setup
1. Add the `com.tngtech.archunit:archunit-junit5` dependency to `backend/build.gradle.kts` (test implementation).

### 2. Layered Architecture Rules
1. Create `ArchitecturalRulesTest` in `backend/src/test/java/com/vantage/`.
2. Define a `LayeredArchitecture` rule:
   - `UI` layer (`..ui..`) must only be accessed by the `App` layer (or act as entry point).
   - `App` layer (`..app..`) may access `UI`, `Domain`, and `Messaging`.
   - `Domain` layer (`..domain..`) must not access `UI`, `App`, or `Messaging`.
   - `Messaging` layer (`..messaging..`) may access `App` and `Domain`.
   - `Infrastructure` layer (`..infrastructure..`) may access `Domain` and `App`.
3. Assert that these layer dependencies are respected.

### 3. Granular Class Rules
1. **Controller Isolation:** Classes annotated with `@RestController` must NOT directly access classes annotated with `@Repository` or interfaces extending `JpaRepository`.
2. **DTO Boundary:** Methods in classes annotated with `@RestController` must NOT return JPA `@Entity` annotated classes. They must return DTOs (e.g., classes in a `..dto..` package or records).
3. **Service Isolation:** Classes annotated with `@Service` must NOT return JPA `@Entity` classes to the UI layer (enforcing the DTO boundary). *Note: If services return entities to internal app classes, that is fine, but not to UI.*
4. **No Field Injection:** Classes must NOT use `@Autowired` on fields. Dependencies must be injected via constructors (enforce `DescribedPredicate` checking for `@Autowired` on `FieldAccessTarget`).

### 4. Verification (Output Instructions)
- Provide the terminal command to run the tests: `./gradlew test --tests "*ArchitecturalRulesTest"`.
- If the test fails due to existing violations, do not modify the main code. Output a report of the violations.

## Target File Paths
- `backend/build.gradle.kts` (Modify to add ArchUnit)
- `backend/src/test/java/com/vantage/ArchitecturalRulesTest.java`
