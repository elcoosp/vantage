
## 90‑Second Demo Script

| Time      | Scene                                                                                                                              |
|-----------|------------------------------------------------------------------------------------------------------------------------------------|
| 0:00‑0:15 | **Intro & React 19 UI** – Show the dashboard with optimistic updates and the global command palette (Cmd+K).                       |
| 0:15‑0:45 | **Flash Sale Concurrency** – Open three browser tabs, simultaneously hit **Buy** on the same product. One succeeds (202), two get **409 Conflict** – inventory versioning works. |
| 0:45‑1:00 | **Chaos Monkey** – Enable payment failure via the admin toggle. Place an order; watch the saga automatically compensate and restore inventory. |
| 1:00‑1:15 | **Observability** – Open Grafana Tempo and show the distributed trace of the failed payment, including the compensating transaction. |
| 1:15‑1:30 | **AI Forecasting** – Navigate to the Forecast dashboard, select a product, and display the 7‑day demand forecast with confidence intervals using Recharts. |
| 1:30‑1:45 | **Live Ops Map** – Show the WebSocket‑driven live map, with pins dropping globally in real‑time as orders are placed.            |
