import { Pause, Play, Radio } from "lucide-react";
import { useEffect, useRef, useState, useTransition } from "react";
import { Card, EmptyState } from "../../components/ui";
import { getToken } from "../../lib/auth";

interface LogEntry {
        id: string;
        timestamp: string;
        method: string;
        path: string;
        statusCode: number;
        durationMs: number;
}

const METHOD_COLORS: Record<string, string> = {
        GET: "text-brand-600 dark:text-brand-300",
        POST: "text-emerald-600 dark:text-emerald-400",
        PUT: "text-amber-600 dark:text-amber-400",
        DELETE: "text-red-600 dark:text-red-400",
};

function methodColor(method: string): string {
        return METHOD_COLORS[method] ?? "text-ink3-light dark:text-ink3-dark";
}

/**
 * Live API request log, streamed from the backend
 * (GET /api/v1/developer/logs/stream, Server-Sent Events).
 *
 * EventSource cannot send custom headers, so the tenant JWT travels via the
 * access_token query parameter (supported by TenantSecurityFilter).
 */
export function ApiLogStream() {
        const [logs, setLogs] = useState<LogEntry[]>([]);
        const [isPaused, setIsPaused] = useState(false);
        const [isConnected, setIsConnected] = useState(false);
        const [, startTransition] = useTransition();
        const containerRef = useRef<HTMLDivElement>(null);
        const pausedRef = useRef(isPaused);
        const bufferRef = useRef<LogEntry[]>([]);
        const flushTimer = useRef<number | null>(null);

        pausedRef.current = isPaused;

        useEffect(() => {
                const token = getToken();
                if (!token) return;

                let source: EventSource | null = null;
                let retryTimer: number | null = null;
                let disposed = false;

                const flush = () => {
                        flushTimer.current = null;
                        const incoming = bufferRef.current;
                        if (incoming.length === 0) return;
                        bufferRef.current = [];
                        startTransition(() => {
                                setLogs((prev) => {
                                        const merged = [...prev, ...incoming];
                                        return merged.length > 200 ? merged.slice(-200) : merged;
                                });
                        });
                };

                const connect = () => {
                        if (disposed) return;
                        source = new EventSource(
                                `/api/v1/developer/logs/stream?replay=50&access_token=${encodeURIComponent(token)}`,
                        );
                        source.addEventListener("log", (event) => {
                                setIsConnected(true);
                                if (pausedRef.current) return; // keep the socket warm, drop rows while paused
                                try {
                                        const raw = JSON.parse((event as MessageEvent).data) as {
                                                id: string;
                                                method: string;
                                                path: string;
                                                status: number;
                                                durationMs: number;
                                                at: string;
                                        };
                                        bufferRef.current.push({
                                                id: raw.id,
                                                timestamp: raw.at,
                                                method: raw.method,
                                                path: raw.path,
                                                statusCode: raw.status,
                                                durationMs: raw.durationMs,
                                        });
                                        if (flushTimer.current === null) {
                                                flushTimer.current = window.setTimeout(flush, 250);
                                        }
                                } catch {
                                        // ignore malformed frames
                                }
                        });
                        source.onopen = () => setIsConnected(true);
                        source.onerror = () => {
                                setIsConnected(false);
                                // EventSource retries automatically; surface the state only.
                        };
                };

                connect();

                return () => {
                        disposed = true;
                        if (retryTimer !== null) window.clearTimeout(retryTimer);
                        if (flushTimer.current !== null) window.clearTimeout(flushTimer.current);
                        source?.close();
                };
        }, []);

        useEffect(() => {
                if (containerRef.current) {
                        containerRef.current.scrollTop = containerRef.current.scrollHeight;
                }
        });

        const clearLogs = () => {
                setLogs([]);
        };

        return (
                <div className="space-y-4">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                                <div className="flex items-center gap-2 text-[13px]">
                                        <span className={`relative flex size-2 ${isConnected ? "text-emerald-500" : "text-ink3-light dark:text-ink3-dark"}`}>
                                                {isConnected && (
                                                        <span className="absolute inline-flex size-full animate-ping rounded-full bg-current opacity-60" />
                                                )}
                                                <span className="relative size-2 rounded-full bg-current" />
                                        </span>
                                        <span
                                                className={`font-medium ${
                                                        isConnected
                                                                ? isPaused
                                                                        ? "text-amber-600 dark:text-amber-400"
                                                                        : "text-emerald-600 dark:text-emerald-400"
                                                                : "text-ink3-light dark:text-ink3-dark"
                                                }`}
                                        >
                                                {isConnected ? (isPaused ? "Paused" : "Live") : "Connecting…"}
                                        </span>
                                </div>
                                <div className="flex items-center gap-2">
                                        <button
                                                type="button"
                                                onClick={() => setIsPaused((prev) => !prev)}
                                                className="inline-flex h-7 items-center gap-1.5 rounded-md bg-card2-light px-2.5 text-[12px] font-medium text-ink2-light transition-colors hover:text-ink-light dark:bg-card2-dark dark:text-ink2-dark dark:hover:text-ink-dark"
                                        >
                                                {isPaused ? <Play className="size-3.5" /> : <Pause className="size-3.5" />}
                                                {isPaused ? "Resume" : "Pause"}
                                        </button>
                                        {logs.length > 0 && (
                                                <button
                                                        type="button"
                                                        onClick={clearLogs}
                                                        className="rounded-md px-2.5 py-1.5 text-[12px] font-medium text-ink3-light transition-colors hover:bg-card2-light hover:text-ink-light dark:text-ink3-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark"
                                                >
                                                        Clear
                                                </button>
                                        )}
                                        <span className="rounded-md bg-card2-light px-2 py-1 font-mono text-[12px] text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
                                                {logs.length}
                                        </span>
                                </div>
                        </div>

                        {logs.length === 0 ? (
                                <Card>
                                        <EmptyState
                                                icon={<Radio className="size-5" />}
                                                title={isConnected && isPaused ? "Stream paused" : "Waiting for API activity"}
                                                description={
                                                        isConnected && isPaused
                                                                ? "Resume to keep receiving API traffic."
                                                                : "Requests to the Vantage API will stream in here in real time."
                                                }
                                        />
                                </Card>
                        ) : (
                                <div
                                        ref={containerRef}
                                        className="h-[420px] overflow-y-auto rounded-xl border border-line-light bg-card-light p-3 font-mono text-[12px] shadow-card custom-scrollbar dark:border-line-dark dark:bg-card-dark"
                                >
                                        <ul className="space-y-0.5">
                                                {logs.map((log) => (
                                                        <li
                                                                key={log.id}
                                                                className="flex items-center gap-3 rounded-md px-2 py-1.5 hover:bg-card2-light/70 dark:hover:bg-card2-dark/70"
                                                        >
                                                                <span className="tabular-nums whitespace-nowrap text-ink3-light dark:text-ink3-dark">
                                                                        {new Date(log.timestamp).toLocaleTimeString()}
                                                                </span>
                                                                <span className={`w-14 font-semibold ${methodColor(log.method)}`}>{log.method}</span>
                                                                <span className="min-w-0 flex-1 truncate text-ink-light dark:text-ink-dark">{log.path}</span>
                                                                <span
                                                                        className={`rounded px-1.5 py-0.5 text-[11px] font-semibold tabular-nums ${
                                                                                log.statusCode >= 500
                                                                                        ? "bg-status-danger-soft-light text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]"
                                                                                        : log.statusCode >= 400
                                                                                                ? "bg-status-warning-soft-light text-status-warning-ink-light dark:bg-[#D3932B]/15 dark:text-[#F0C468]"
                                                                                                : "bg-status-success-soft-light text-status-success-ink-light dark:bg-[#1EAD72]/15 dark:text-[#6CD6AB]"
                                                                        }`}
                                                                >
                                                                        {log.statusCode}
                                                                </span>
                                                                <span className="w-16 text-right tabular-nums text-ink3-light dark:text-ink3-dark">
                                                                        {log.durationMs}ms
                                                                </span>
                                                        </li>
                                                ))}
                                        </ul>
                                </div>
                        )}
                </div>
        );
}
