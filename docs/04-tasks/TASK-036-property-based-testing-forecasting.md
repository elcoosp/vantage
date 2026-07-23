# TASK-036: Implement Property-Based Testing for Forecasting Algorithm

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Engineering Maturity)
- Read: `docs/04-tasks/TASK-010-ai-demand-forecasting.md` (The system under test)

## Objective
Elevate the testing strategy from traditional example-based tests to Property-Based Testing (PBT). Instead of testing specific inputs, the PBT framework (jqwik) will generate thousands of randomized 30-day historical data arrays and assert that the Holt-Winters forecasting algorithm adheres to fundamental mathematical invariants (properties) without ever crashing or returning negative bounds. This demonstrates elite-level Java testing maturity.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/test/java/com/vantage/analytics/` and `backend/build.gradle.kts` (to add the jqwik dependency).
- DO NOT modify the main source code of the forecasting algorithm or any other module.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/java/com/vantage/analytics/app/HoltWintersForecastCalculator.java`
- `backend/build.gradle.kts`

## Acceptance Criteria

### 1. Dependency Setup
1. Add the `jqwik` dependency to `backend/build.gradle.kts` (e.g., `net.jqwik:jqwik:1.8.2`).
2. Configure the `test` task to use the JUnit Platform (if not already configured for jqwik).

### 2. Property-Based Test Implementation
1. Create `ForecastPropertyTest` in `backend/src/test/java/com/vantage/analytics/`.
2. Annotate the class with `@Property(generation = GenerationMode.RANDOMIZED, tries = 1000)` to run 1,000 randomized iterations.
3. Provide an Arbitrary for the historical data: a `double[]` of length 30 where values range from 0.0 to 1000.0.
4. **Property 1 (Non-negativity):** Assert that for any generated historical data, the `lowerBound` of every forecasted day is `>= 0.0`.
5. **Property 2 (Valid Interval):** Assert that for every forecasted day, `lowerBound <= predictedQuantity <= upperBound`.
6. **Property 3 (Robustness):** Assert that the `HoltWintersForecastCalculator` never throws an exception (e.g., `IndexOutOfBounds`, `ArithmeticException`) for any valid random input array.

### 3. Verification (Output Instructions)
- Provide the terminal command to run the property tests: `./gradlew test --tests "*ForecastPropertyTest"`.
- Confirm that the build fails if any of the 1,000 iterations violate the properties.

## Target File Paths
- `backend/build.gradle.kts` (Modify to add jqwik)
- `backend/src/test/java/com/vantage/analytics/ForecastPropertyTest.java`
