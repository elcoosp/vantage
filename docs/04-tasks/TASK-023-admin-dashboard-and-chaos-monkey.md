# TASK-023: Implement Admin Dashboard & Chaos Monkey Control Panel

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 2 - The Platform Admin)
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Distributed Order Engine, Resilience and Fault Tolerance)

## Objective
Implement a Platform Admin dashboard and a "Chaos Monkey" control panel. The Admin dashboard will display global system metrics (total vendors, total orders, system-wide circuit breaker states). The Chaos Monkey control panel will allow the Admin to toggle the "Simulate Payment Gateway Failure" flag at runtime, instantly triggering Saga compensating transactions for subsequent orders, providing a dramatic live demo experience.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/admin/`, `backend/src/main/java/com/vantage/payment/` (to read the flag), `frontend/src/features/admin/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or domain logic in other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/java/com/vantage/payment/infrastructure/MockPaymentGatewayClient.java`
- `frontend/src/components/Layout.tsx`
- `frontend/src/lib/api.ts`

## Acceptance Criteria

### 1. Backend Chaos Monkey State Management
1. Create `ChaosMonkeyService` in `com.vantage.core.admin.app`.
   - This service must hold a static `AtomicBoolean` flag (or a database-backed setting) to control the payment gateway failure simulation.
   - Provide methods `enablePaymentFailure()`, `disablePaymentFailure()`, and `isPaymentFailureEnabled()`.
2. Modify `MockPaymentGatewayClient` in `com.vantage.payment.infrastructure` to check `ChaosMonkeyService.isPaymentFailureEnabled()` before processing a payment. If enabled, it must immediately throw `PaymentGatewayException` to trigger the Resilience4j fallback and subsequent Saga compensation.
3. Create `AdminController` in `com.vantage.core.admin.ui` exposing:
   - `POST /api/v1/admin/chaos-monkey/payment-failure` (Body: `{ "enabled": true }`).
   - `GET /api/v1/admin/chaos-monkey/payment-failure`.
4. Secure these endpoints so only users with the `ROLE_ADMIN` can access them.

### 2. Backend System Metrics Endpoint
1. Add a method to `AdminController` exposing `GET /api/v1/admin/metrics`.
2. Return a `SystemMetricsResponse` containing:
   - `totalVendors` (Count from `VendorRepository`)
   - `totalOrders` (Count from `OrderRepository`)
   - `paymentCircuitBreakerState` (Read from Resilience4j `CircuitBreakerRegistry`)

### 3. Frontend Admin Dashboard
1. Create `frontend/src/features/admin/AdminDashboard.tsx`.
2. Fetch system metrics using React Query every 5 seconds.
3. Display the metrics in a grid of stat cards (Total Vendors, Total Orders, Circuit Breaker State).
4. Add a prominent "Chaos Monkey" control card.
5. Include a toggle switch for "Simulate Payment Gateway Failure".
6. When toggled ON, call `POST /api/v1/admin/chaos-monkey/payment-failure` with `{ "enabled": true }`.
7. Display a red warning badge on the dashboard when the Chaos Monkey is active.

### 4. Verification (Output Instructions)
- Provide instructions on how to test the flow: Toggle the Chaos Monkey ON -> Place an order -> Observe the Saga compensation triggering immediately -> Toggle it OFF -> Place another order -> Observe success.

## Target File Paths
- `backend/src/main/java/com/vantage/core/admin/app/ChaosMonkeyService.java`
- `backend/src/main/java/com/vantage/core/admin/ui/AdminController.java`
- `backend/src/main/java/com/vantage/core/admin/ui/dto/SystemMetricsResponse.java`
- `backend/src/main/java/com/vantage/core/admin/ui/dto/ChaosMonkeyToggleRequest.java`
- `backend/src/main/java/com/vantage/payment/infrastructure/MockPaymentGatewayClient.java` (Modify)
- `frontend/src/features/admin/AdminDashboard.tsx`
- `frontend/src/features/admin/useAdminMetrics.ts`
- `frontend/src/features/admin/useChaosMonkey.ts`
