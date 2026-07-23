# TASK-014: Implement AI Forecast Visualization (React 19 + Recharts)

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: AI Demand Forecasting)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 5.2: Forecast Visualization)

## Objective
Implement the frontend dashboard for the AI Demand Forecasting feature. The UI must fetch the 7-day forecast data from the backend and render it using Recharts. The chart must display historical data (solid line), forecasted data (dashed line), and a shaded confidence interval band.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ForecastResponse`, `ForecastDataPoint`)
- `frontend/src/lib/api.ts`
- `frontend/src/components/Layout.tsx`

## Acceptance Criteria

### 1. Dependencies & Setup
1. Install `recharts` and `date-fns` in the `frontend/` directory.
2. Create a new route `/forecast` in the application router.

### 2. Data Fetching
1. Create `frontend/src/features/analytics/useForecast.ts` custom hook.
2. Use `@tanstack/react-query` to call `GET /api/v1/analytics/forecast/{productId}`.
3. The hook must accept a `productId` parameter.

### 3. Chart Implementation
1. Create `frontend/src/features/analytics/ForecastChart.tsx`.
2. Use the `ComposedChart` component from Recharts.
3. X-Axis: Date (formatted as "MMM dd").
4. Y-Axis: Quantity.
5. Render a shaded `<Area>` representing the confidence interval (`upperBound` to `lowerBound`).
6. Render a solid `<Line>` for historical actual quantities (if passed, or just rely on forecast data point).
7. Render a dashed `<Line>` for `predictedQuantity`.
8. Add a `<Tooltip>` that displays the date, predicted quantity, and the confidence range.
9. Add a `<Legend>` to differentiate between historical, forecast, and confidence interval.

### 4. UI Integration
1. Create `frontend/src/features/analytics/ForecastDashboard.tsx`.
2. Include a dropdown/select menu to choose a product.
3. When a product is selected, render the `ForecastChart` with the fetched data.
4. Display a loading spinner while data is fetching, and an error message if the API call fails.

### 5. Verification (Output Instructions)
- Provide the terminal commands to install the new npm dependencies (`recharts`, `date-fns`).
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/features/analytics/ForecastDashboard.tsx`
- `frontend/src/features/analytics/ForecastChart.tsx`
- `frontend/src/features/analytics/useForecast.ts`
