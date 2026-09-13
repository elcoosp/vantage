import { Circle } from "lucide-react";
import { MapView } from "./MapView";
import { useOpsMapSocket } from "./useOpsMapSocket";

export function OpsDashboard() {
	const { pins, isConnected } = useOpsMapSocket();

	return (
		<div className="relative isolate h-[calc(100vh-140px)] overflow-hidden rounded-2xl border border-line-light shadow-card dark:border-line-dark">
			<MapView pins={pins} />

			<div className="pointer-events-none absolute left-4 top-4 flex items-center gap-2 rounded-lg border border-line-light/60 bg-card-light/80 px-3 py-2 font-mono text-[12px] text-ink-light shadow-pop backdrop-blur-md dark:border-line-dark/60 dark:bg-card-dark/80 dark:text-ink-dark">
				<span className="size-1.5 rounded-full bg-brand-500" />
				Live shipments: <span className="font-bold tabular-nums text-brand-600 dark:text-brand-400">{pins.length}</span>
			</div>

			{!isConnected && (
				<div className="pointer-events-none absolute right-4 top-4 flex items-center gap-2 rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light/90 px-3 py-2 font-mono text-[12px] text-status-danger-ink-light shadow-pop backdrop-blur-md animate-pulse dark:border-[#E5484D]/30 dark:bg-[#E5484D]/20 dark:text-[#FF9A9D]">
					<Circle className="size-3 fill-current" />
					Reconnecting…
				</div>
			)}
		</div>
	);
}
