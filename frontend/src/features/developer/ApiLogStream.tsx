import { Pause, Play, Radio } from "lucide-react";
import { useEffect, useRef, useState, useTransition } from "react";
import { Card, EmptyState } from "../../components/ui";

interface LogEntry {
	timestamp: string;
	method: string;
	path: string;
	statusCode: number;
	durationMs: number;
}

function generateMockLog(): LogEntry {
	const methods = ["GET", "POST", "PUT", "DELETE"];
	const paths = ["/api/v1/orders", "/api/v1/products", "/api/v1/inventory", "/api/v1/payments", "/api/v1/analytics"];
	const statuses = [200, 201, 200, 200, 400, 401, 404, 500];
	return {
		timestamp: new Date().toISOString(),
		method: methods[Math.floor(Math.random() * methods.length)],
		path: paths[Math.floor(Math.random() * paths.length)],
		statusCode: statuses[Math.floor(Math.random() * statuses.length)],
		durationMs: Math.floor(Math.random() * 500) + 10,
	};
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

export function ApiLogStream() {
	const [logs, setLogs] = useState<LogEntry[]>([]);
	const [isPaused, setIsPaused] = useState(false);
	const [, startTransition] = useTransition();
	const containerRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		let interval: number | null = null;
		if (!isPaused) {
			interval = window.setInterval(() => {
				const newLog = generateMockLog();
				startTransition(() => {
					setLogs((prev) => (prev.length > 100 ? [...prev.slice(-99), newLog] : [...prev, newLog]));
				});
			}, 1000);
		}
		return () => {
			if (interval) clearInterval(interval);
		};
	}, [isPaused]);

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
					<span className="relative flex size-2 text-emerald-500">
						<span className="absolute inline-flex size-full animate-ping rounded-full bg-current opacity-60" />
						<span className="relative size-2 rounded-full bg-current" />
					</span>
					<span className="font-medium text-emerald-600 dark:text-emerald-400">{isPaused ? "Paused" : "Live"}</span>
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
						title={isPaused ? "Stream paused" : "Waiting for API activity"}
						description={
							isPaused
								? "Resume to keep receiving simulated API traffic."
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
						{logs.map((log, index) => (
							<li
								key={`${log.timestamp}-${index}`}
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
