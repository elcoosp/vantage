# TASK-026: Implement Streaming LLM Support Chat (SSE)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Enhance vendor support experience)
- Read: `docs/00-product/03-epics-and-user-stories.md` (Modern UI/UX requirements)

## Objective
Implement a "Support Assistant" chat widget in the vendor dashboard. The backend will expose a Server-Sent Events (SSE) endpoint that streams a canned or simple algorithmic response token-by-token. The React 19 frontend will consume this stream and render the tokens in real-time, demonstrating advanced asynchronous UI handling and modern web platform APIs.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/chat/` and `frontend/src/features/chat/`.
- DO NOT modify `application.yml`, `build.gradle.kts`, or other domain modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `frontend/src/components/Layout.tsx`

## Acceptance Criteria

### 1. Backend SSE Endpoint
1. Create `ChatController` in `com.vantage.core.chat.ui` exposing `GET /api/v1/chat/stream`.
2. The endpoint must accept a `query` parameter and return `SseEmitter`.
3. Configure the emitter with a long timeout (e.g., 5 minutes).
4. Use a Java 21 Virtual Thread (`Thread.startVirtualThread`) to simulate an LLM generating a response.
5. The virtual thread should split a predefined canned response (or a simple echo logic) into word-by-word chunks.
6. Send each chunk as an SSE event (`event: message`, `data: <chunk>`).
7. Add a slight delay (e.g., `Thread.sleep(50)`) between chunks to simulate token generation latency.
8. Complete the emitter gracefully when finished, or send an error event on failure.

### 2. Frontend Chat Widget
1. Create `frontend/src/features/chat/ChatWidget.tsx`.
2. Render a floating chat button in the bottom right corner of the `Layout.tsx`.
3. Clicking the button opens a chat panel.
4. Allow the user to type a query and hit "Send".
5. Use the native `EventSource` API (or `fetch` with `ReadableStream` if custom headers are strictly required, though `EventSource` is preferred for simplicity) to connect to the SSE endpoint.
6. As events arrive, append the chunks to the chat message state.
7. Use React 19's `useTransition` when updating the chat state to ensure the input field remains perfectly responsive while the message streams in.

### 3. UI Polish
1. Style the chat panel to look like modern AI chat interfaces (dark mode, user messages on right, assistant on left).
2. Show a blinking cursor or "typing..." indicator while the SSE stream is active.
3. Disable the "Send" button while the response is streaming.

### 4. Verification (Output Instructions)
- Provide the command to run both backend and frontend.
- Verify that sending a message results in a word-by-word streaming response in the UI.

## Target File Paths
- `backend/src/main/java/com/vantage/core/chat/ui/ChatController.java`
- `backend/src/main/java/com/vantage/core/chat/app/ChatService.java`
- `frontend/src/features/chat/ChatWidget.tsx`
- `frontend/src/features/chat/useChatStream.ts`
