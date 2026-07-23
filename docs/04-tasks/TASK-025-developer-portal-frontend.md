# TASK-025: Implement Developer Portal Frontend (API Keys & Webhook Logs)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 3 - The External Developer)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 4.1 & 4.2: API Keys and Webhooks)

## Objective
Implement the "Developer Portal" UI in the React 19 frontend. This dashboard tab allows vendors to manage their platform integrations. It includes a UI to generate and reveal API keys (with a strict "shown only once" UX), configure webhook endpoints, and view a streaming, real-time log of incoming API requests and webhook delivery attempts.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ApiKeyResponse`, `WebhookUpdateRequest`)
- `frontend/src/lib/api.ts`
- `frontend/src/components/Layout.tsx`

## Acceptance Criteria

### 1. API Key Management UI
1. Create `frontend/src/features/developer/ApiKeysPanel.tsx`.
2. Fetch existing API keys via `GET /api/v1/api-keys`. Display them in a table showing `name`, `keyPrefix`, `lastUsedAt`, and a "Revoke" button.
3. Include a "Generate New Key" button that opens a modal asking for a key name.
4. On submit, call `POST /api/v1/api-keys`.
5. **CRITICAL UX:** Display the full plaintext key in a success modal with a "Copy to Clipboard" button. Show a warning: "This key will not be shown again. Please store it securely."
6. Implement the revoke action by calling `DELETE /api/v1/api-keys/{id}` and invalidating the React Query cache.

### 2. Webhook Configuration UI
1. Create `frontend/src/features/developer/WebhooksPanel.tsx`.
2. Fetch the current webhook configuration via `GET /api/v1/webhooks`.
3. Provide a form to update the `webhookUrl` via `PUT /api/v1/webhooks`.
4. If the `webhookSecret` is not set, allow the user to generate one. Display the secret exactly once with a copy button.

### 3. Live API Request Log (Streaming)
1. Create `frontend/src/features/developer/ApiLogStream.tsx`.
2. Connect to a backend WebSocket or Server-Sent Events (SSE) endpoint streaming API request logs (Note: If backend endpoint doesn't exist, mock this stream locally with a `setInterval` generating fake log entries for the demo, but structure the code to easily swap to a real SSE connection).
3. Render an auto-scrolling, terminal-style list of incoming requests, including `timestamp`, `method`, `path`, `status`, and `latencyMs`.
4. Use React 19's `useTransition` to ensure the UI remains perfectly smooth while the log updates rapidly.

### 4. Developer Portal Layout
1. Create `frontend/src/features/developer/DeveloperPortal.tsx`.
2. Combine `ApiKeysPanel`, `WebhooksPanel`, and `ApiLogStream` into a clean, tabbed or grid layout.
3. Add a "Developer Portal" route to the main application router.

### 5. Verification (Output Instructions)
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/features/developer/DeveloperPortal.tsx`
- `frontend/src/features/developer/ApiKeysPanel.tsx`
- `frontend/src/features/developer/WebhooksPanel.tsx`
- `frontend/src/features/developer/ApiLogStream.tsx`
- `frontend/src/features/developer/useApiKeys.ts`
- `frontend/src/features/developer/useWebhooks.ts`
