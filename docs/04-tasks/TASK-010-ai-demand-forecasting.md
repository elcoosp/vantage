# TASK-010: Implement Pure-Java AI Demand Forecasting

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: AI Demand Forecasting)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 5.1: Pure-Java Sales Forecasting)

## Objective
Implement the analytics module to generate a 7-day demand forecast for a specific product. The system must query the last 30 days of order history for the tenant and apply a pure-Java implementation of the Holt-Winters Exponential Smoothing algorithm to calculate trend and seasonality, returning predicted quantities and a confidence interval.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/analytics/` and its corresponding test directory.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ForecastResponse`)
- `docs/02-contracts/03-database-schema.md` (Reference: `orders` table)
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/order/domain/OrderStatus.java`

## Acceptance Criteria

### 1. Data Aggregation
1. Create `AnalyticsService` in `com.vantage.analytics.app`.
2. Query the `orders` table for the given `productId` and `tenantId` (via Hibernate filter).
3. Aggregate the ordered quantities by day for the past 30 days. If a day has no sales, the quantity is 0.
4. Return the data as a `double[]` array representing the 30-day historical time series.

### 2. Holt-Winters Exponential Smoothing Algorithm
1. Create `HoltWintersForecastCalculator` in `com.vantage.analytics.app`.
2. Implement the Triple Exponential Smoothing algorithm in pure Java.
   - Parameters: `alpha` (level), `beta` (trend), `gamma` (seasonality). Use sensible defaults (e.g., 0.3, 0.1, 0.3) or simple grid search optimization based on historical error.
   - Assume a seasonality period of 7 days (weekly).
3. The calculator must forecast the next 7 days of demand.
4. Calculate a 95% confidence interval (upper and lower bounds) based on the Mean Squared Error (MSE) of the historical forecast.

### 3. REST API Implementation
1. Create `AnalyticsController` in `com.vantage.analytics.ui` exposing `GET /api/v1/analytics/forecast/{productId}`.
2. Call `AnalyticsService` to get the historical data and forecast.
3. Map the result to the `ForecastResponse` schema.
4. The response must contain an array of 7 objects, each with `date`, `predictedQuantity` (int), `lowerBound` (int), and `upperBound` (int).
5. Quantities must be non-negative (do not return negative lower bounds; floor at 0).

### 4. Integration Testing (Testcontainers)
1. Create `ForecastAnalyticsIT` in `backend/src/test/java/com/vantage/analytics/`.
2. Setup: Register a vendor, create a product.
3. Insert 30 days of mock `orders` into the database with varying quantities (e.g., higher sales on weekends to test seasonality).
4. Call `GET /api/v1/analytics/forecast/{productId}`.
5. Verify the response returns a 200 OK.
6. Verify the response array contains exactly 7 days of forecasts.
7. Verify the `predictedQuantity` falls within the `[lowerBound, upperBound]` range.
8. Verify the `lowerBound` is never negative.

## Target File Paths
- `backend/src/main/java/com/vantage/analytics/app/AnalyticsService.java`
- `backend/src/main/java/com/vantage/analytics/app/HoltWintersForecastCalculator.java`
- `backend/src/main/java/com/vantage/analytics/ui/AnalyticsController.java`
- `backend/src/main/java/com/vantage/analytics/ui/dto/ForecastResponse.java`
- `backend/src/main/java/com/vantage/analytics/ui/dto/ForecastDataPoint.java`
- `backend/src/test/java/com/vantage/analytics/ForecastAnalyticsIT.java`
