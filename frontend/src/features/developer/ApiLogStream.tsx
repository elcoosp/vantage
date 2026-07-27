import { useEffect, useRef, useState, useTransition } from "react";

interface LogEntry {
	timestamp: string;
	method: string;
	path: string;
	status: number;
	latencyMs: number;
}

// Mock log generator – replace with real SSE/WebSocket connection
function generateMockLog(): LogEntry {
	const methods = ["GET", "POST", "PUT", "DELETE"];
	const paths = ["/api/v1/orders", "/api/v1/products", "/api/v1/inventory", "/api/v1/payments", "/api/v1/analytics"];
	const statuses = [200, 201, 400, 401, 404, 500];
	return {
		timestamp: new Date().toISOString(),
		method: methods[Math.floor(Math.random() * methods.length)],
		path: paths[Math.floor(Math.random() * paths.length)],
		status: statuses[Math.floor(Math.random() * statuses.length)],
		latencyMs: Math.floor(Math.random() * 500) + 10,
	};
}

export function ApiLogStream() {
	const [logs, setLogs] = useState<LogEntry[]>([]);
	const [, startTransition] = useTransition();
	const containerRef = useRef<HTMLDivElement>(null);
	const [isPaused, setIsPaused] = useState(false);

	useEffect(() => {
		let interval: number | null = null;
		if (!isPaused) {
			interval = window.setInterval(() => {
				const newLog = generateMockLog();
				startTransition(() => {
					setLogs((prev) => {
						// Keep last 100 entries
						const updated = [...prev, newLog];
						if (updated.length > 100) {
							return updated.slice(-100);
						}
						return updated;
					});
				});
			}, 1000);
		}
		return () => {
			if (interval) clearInterval(interval);
		};
	}, [isPaused]);

	// Auto-scroll to bottom when logs change
	useEffect(() => {
		if (containerRef.current) {
			containerRef.current.scrollTop = containerRef.current.scrollHeight;
		}
	});

	const togglePause = () => {
		setIsPaused((prev) => !prev);
	};

	const clearLogs = () => {
		setLogs([]);
	};

	const getStatusColor = (status: number) => {
		if (status < 300) return "text-green-400";
		if (status < 400) return "text-yellow-400";
		return "text-red-400";
	};

	return (
		<div>
			<div className="flex justify-between items-center mb-2">
				<h3 className="text-lg font-semibold">Live API Request Log</h3>
				<div className="flex gap-2">
					<button
						type="button"
						onClick={togglePause}
						className="px-3 py-1 text-sm bg-gray-600 text-white rounded hover:bg-gray-700"
					>
						{isPaused ? "Resume" : "Pause"}
					</button>
					<button
						type="button"
						onClick={clearLogs}
						className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700"
					>
						Clear
					</button>
				</div>
			</div>
			<div
				ref={containerRef}
				className="bg-black text-gray-200 font-mono text-sm p-4 rounded-lg h-[400px] overflow-y-auto border border-gray-700"
			>
				{logs.length === 0 && <div className="text-gray-500">Waiting for logs...</div>}
				{logs.map((log, index) => (
					<div key={`${log.timestamp}-${index}`} className="py-0.5 flex gap-4">
						<span className="text-gray-500 whitespace-nowrap">{new Date(log.timestamp).toLocaleTimeString()}</span>
						<span className="font-bold text-blue-400 w-12">{log.method}</span>
						<span className="text-gray-300 flex-1">{log.path}</span>
						<span className={`font-semibold w-16 ${getStatusColor(log.status)}`}>{log.status}</span>
						<span className="text-gray-500 w-16 text-right">{log.latencyMs}ms</span>
					</div>
				))}
			</div>
		</div>
	);
}
