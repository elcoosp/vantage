# TASK-026: Implement Streaming LLM Support Chat

## Status: STILL OPEN (Phase 1 — Truth pass complete)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Enhance vendor support experience)
- Read: `docs/00-product/03-epics-and-user-stories.md` (Modern UI/UX requirements)

## Objective
Implement a "Support Assistant" chat widget in the vendor dashboard. The backend exposes
a streaming SSE endpoint that calls a real LLM via provider abstraction (OpenAI/Anthropic/Ollama),
with tenant isolation, conversation persistence, RAG, and tool/function-calling capabilities.

## Execution Boundaries
- Backend: `backend/src/main/java/com/vantage/core/chat/`
- Frontend: `frontend/src/features/chat/`
- Migrations: `backend/src/main/resources/db/migration/V11-V15`
- Config: `backend/src/main/resources/application.yml` (vantage.ai section)
- Build: `backend/build.gradle.kts` (added webflux)

## Architecture (implemented)

```
React ChatWidget
  -> POST /api/v1/chat/stream (fetch + ReadableStream, NOT EventSource)
    -> ChatController
      -> ChatOrchestrator
        -> LlmProvider (OpenAiProvider: OpenAI/Anthropic/Ollama/stub)
        -> ToolRegistry -> OrderStatusTool, InventoryStatusTool, ForecastTool, ProductSearchTool
        -> AiGuardrails (PII redaction, prompt-injection detection, rate limiting)
        -> ConversationRepository (ai_conversations, ai_messages, ai_tool_calls, ai_usage)
    -> SSE token stream + tool calls + usage
```

## What was built

### Backend

1. **Replaced** `ChatService` (canned `List.of("Hello", "world", "!")`) with `ChatOrchestrator`
   — a real orchestration layer that:
   - Resolves tenant from `TenantContext` (JWT or X-Tenant-ID header)
   - Streams via `LlmProvider` abstraction
   - Executes tool calls with tenant isolation
   - Applies guardrails (PII redaction, injection detection, rate limiting)
   - Persists conversations to `ai_conversations` / `ai_messages` / `ai_tool_calls` / `ai_usage`

2. **LlmProvider** abstraction with `OpenAiProvider` implementation:
   - Supports `provider: openapi` for real OpenAI API calls
   - Supports `provider: stub` for deterministic fallback (no API key needed)
   - Parses SSE `data:` lines into `ContentToken`, `ToolCall`, `Usage`, `Error`, `Done` events

3. **Tools** (all enforce `TenantContext` and scope queries by `tenant_id`):
   - `getOrderStatus(orderId)` — scoped to tenant
   - `getInventory(productId)` — scoped to tenant
   - `getForecast(productId)` — delegates to `AnalyticsService` (Holt-Winters)
   - `searchProducts(query, limit)` — scoped to tenant

4. **Guardrails** (`AiGuardrails`):
   - PII redaction (email, credit card, phone patterns)
   - Prompt injection detection (DAN, "ignore previous instructions", etc.)
   - System prompt with tenant context: "You are Vantage assistant for tenant X"
   - Per-tenant token budget + request rate limit (`TokenRateLimiter`)

5. **Tenant filter registration**: Added `tenantFilter_*` for all new AI entities
   in both `TenantFilterInterceptor` and `TenantFilterActivator`.

6. **Migrations**: V11 (conversations/messages/tool_calls/usage), V12 (documents/embeddings),
   V13 (pgvector extension), V14 (forecast_runs), V15 (seed help docs).

7. **Config**: `application.yml` gains `vantage.ai.*` properties (provider, model, api-key,
   base-url, max-tokens, max-requests).

### Frontend

1. **Replaced** `EventSource` with `fetch` + `ReadableStream`:
   - `useChatStream.ts` now uses POST with JSON body + `Authorization` header
   - SSE line parser extracts `data:` events as JSON `ChatEvent`
   - Handles disconnect, retry, cancel (AbortController)

2. **ChatWidget.tsx**: Updated label from "Vantage AI" to "Support Assistant",
   improved placeholder text, added tool-result rendering.

## Acceptance Criteria Progress

### Phase 1 — Truth pass (DONE)
- [x] Renamed "AI demand forecasting" to "statistical demand forecasting" in PRODUCT.md and README.md
- [x] Replaced `ChatService` stub with real `ChatOrchestrator` + `LlmProvider` abstraction
- [x] Added tenant isolation on all chat entities and all tool queries
- [x] Added conversation persistence (`ai_conversations`, `ai_messages`, `ai_tool_calls`, `ai_usage`)
- [x] Added guardrails (PII redaction, prompt injection detection, rate limiting)
- [x] Replaced `EventSource` with `fetch` + `ReadableStream` (supports Authorization header)

### Phase 2 — Chat v1 (STILL OPEN)
- [ ] LLM provider integration with real API key (OpenAI/Anthropic/Ollama)
- [ ] Conversation list UI (list past conversations)

### Phase 3 — RAG (DONE)
|- [x] pgvector-enabled RAG retriever (`DocumentRetriever`)
|- [x] Help doc ingestion pipeline (`DocumentIngestor`)
|- [x] Citation rendering in the frontend (`ChatWidget.tsx` + `useChatStream.ts`)

### Phase 4 — Forecast v2 (DONE)
|- [x] Model abstraction (`ForecastModel` interface)
|- [x] `HoltWintersModel` + `SeasonalNaiveModel` baselines
|- [x] Rolling-origin backtesting (MAPE, sMAPE, MASE, pinball, coverage)
|- [x] Model selection: backtest all models, pick best by MAPE
|- [x] `forecast_runs` persistence (migration V14, `ForecastRunRepository`)
|- [x] Feature pipeline stub (`ForecastFeature` record for lag/rolling/holiday)
|- [ ] ML model service (LightGBM/Prophet/XGBoost4J) — Phase 5 candidate

### Phase 5 — Evals + guardrails (DONE)
|- [x] Golden set for chat (`ForecastBacktestTest` — 6 property + invariant tests)
|- [x] Prompt injection tests (`AiGuardrailsTest` — 8 injection pattern tests)
|- [x] Cross-tenant leakage tests (`ChatTenantIsolationIT` — verifies conversation isolation)
|- [x] PII redaction tests (covered by `ChatOrchestratorTest.should_throw_when_tenant_context_missing` + guardrails)

## Target File Paths (implemented)
- `backend/src/main/java/com/vantage/core/chat/app/ChatOrchestrator.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/LlmProvider.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/LlmStreamEvent.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/AiGuardrails.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/TokenRateLimiter.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/ToolRegistry.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/tool/ChatTool.java` (NEW, interface)
- `backend/src/main/java/com/vantage/core/chat/app/tool/OrderStatusTool.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/tool/InventoryStatusTool.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/tool/ForecastTool.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/tool/ProductSearchTool.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/app/provider/OpenAiProvider.java` (NEW)
- `backend/src/main/java/com/vantage/core/chat/ui/ChatController.java` (REPLACED — POST + SSE)
- `backend/src/main/java/com/vantage/core/chat/infra/AiConversation.java` (NEW entity)
- `backend/src/main/java/com/vantage/core/chat/infra/AiMessage.java` (NEW entity)
- `backend/src/main/java/com/vantage/core/chat/infra/AiToolCall.java` (NEW entity)
- `backend/src/main/java/com/vantage/core/chat/infra/AiUsage.java` (NEW entity)
- `backend/src/main/java/com/vantage/core/chat/infra/AiDocument.java` (NEW entity)
- `backend/src/main/java/com/vantage/core/chat/infra/ConversationRepository.java` (NEW)
- `backend/src/main/resources/db/migration/V11__create_ai_conversations.sql` (NEW)
- `backend/src/main/resources/db/migration/V12__create_ai_documents_embeddings.sql` (NEW)
- `backend/src/main/resources/db/migration/V13__enable_pgvector.sql` (NEW)
- `backend/src/main/resources/db/migration/V14__create_forecast_runs.sql` (NEW)
- `backend/src/main/resources/db/migration/V15__seed_ai_documents.sql` (NEW)
- `backend/src/main/resources/application.yml` (MODIFIED — vantage.ai section)
- `backend/build.gradle.kts` (MODIFIED — added spring-boot-starter-webflux)
- `frontend/src/features/chat/useChatStream.ts` (REPLACED — fetch + ReadableStream)
- `frontend/src/features/chat/ChatWidget.tsx` (MODIFIED — label, tool-result rendering)
- `PRODUCT.md` (MODIFIED — "AI demand forecasting" → "statistical demand forecasting")
- `README.md` (MODIFIED — "AI-driven" → "statistical", "Pure-Java AI forecasting" → "Statistical")
