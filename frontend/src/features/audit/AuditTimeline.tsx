import { useState } from "react";
import type { AuditEvent } from "./useOrderAudit";

const EVENT_COLORS: Record<string, string> = {
	ORDER_CREATED: "blue",
	INVENTORY_RESERVED: "green",
	PAYMENT_SUCCEEDED: "green",
	PAYMENT_FAILED: "red",
	ORDER_CANCELLED: "red",
	INVENTORY_RELEASED: "orange",
};

const EVENT_LABELS: Record<string, string> = {
	ORDER_CREATED: "Order Created",
	INVENTORY_RESERVED: "Inventory Reserved",
	PAYMENT_SUCCEEDED: "Payment Succeeded",
	PAYMENT_FAILED: "Payment Failed",
	ORDER_CANCELLED: "Order Cancelled",
	INVENTORY_RELEASED: "Inventory Released",
};

interface AuditTimelineProps {
	events: AuditEvent[];
}

function getColorClass(eventType: string): string {
  const color = EVENT_COLORS[eventType] || "gray";
  const map: Record<string, string> = {
    blue: "bg-blue-500 border-blue-500",
    green: "bg-green-500 border-green-500",
    red: "bg-red-500 border-red-500",
    orange: "bg-orange-500 border-orange-500",
    gray: "bg-gray-500 border-gray-500",
  };
  return map[color] || "bg-gray-500 border-gray-500";
}

export function AuditTimeline({ events }: AuditTimelineProps) {
	const [expandedId, setExpandedId] = useState<string | null>(null);

	if (events.length === 0) {
		return <div className="text-slate-400 text-sm">No audit events found.</div>;
	}

	return (
		<div className="relative pl-6 space-y-6">
			{events.map((event, index) => {
				const isExpanded = expandedId === event.id;
				const colorClass = getColorClass(event.eventType);
				const label = EVENT_LABELS[event.eventType] || event.eventType;

				return (
					<div key={event.id} className="relative">
						{index < events.length - 1 && <div className="absolute left-[-8px] top-6 bottom-0 w-0.5 bg-slate-600" />}
						<div className="flex items-start gap-3">
							<div className={`w-4 h-4 rounded-full border-2 ${colorClass} shrink-0 mt-1 z-10`} />
							<div className="flex-1 min-w-0">
								<div className="flex flex-wrap items-center gap-2">
									<span className="text-sm font-semibold text-slate-200">{label}</span>
									<span className="text-xs text-slate-400">{new Date(event.createdAt).toLocaleString()}</span>
								</div>
								<button
									type="button"
									onClick={() => setExpandedId(isExpanded ? null : event.id)}
									className="text-xs text-blue-400 hover:text-blue-300 mt-1"
								>
									{isExpanded ? "Hide Details" : "View Details"}
								</button>
								{isExpanded && (
									<pre className="mt-2 p-3 bg-slate-900 rounded text-xs text-slate-300 overflow-x-auto border border-slate-700 max-h-60">
										{(() => {
											try {
												const parsed = JSON.parse(event.payload);
												return JSON.stringify(parsed, null, 2);
											} catch {
												return event.payload;
											}
										})()}
									</pre>
								)}
							</div>
						</div>
					</div>
				);
			})}
		</div>
	);
}
