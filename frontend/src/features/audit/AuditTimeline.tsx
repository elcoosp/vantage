import { useState } from "react";
import type { AuditEvent } from "./useOrderAudit";

const EVENT_TONES: Record<string, "success" | "danger" | "warning" | "info" | "muted"> = {
	ORDER_CREATED: "info",
	INVENTORY_RESERVED: "success",
	PAYMENT_SUCCEEDED: "success",
	PAYMENT_FAILED: "danger",
	ORDER_CANCELLED: "danger",
	INVENTORY_RELEASED: "warning",
};

const EVENT_LABELS: Record<string, string> = {
	ORDER_CREATED: "Order created",
	INVENTORY_RESERVED: "Inventory reserved",
	PAYMENT_SUCCEEDED: "Payment succeeded",
	PAYMENT_FAILED: "Payment failed",
	ORDER_CANCELLED: "Order cancelled",
	INVENTORY_RELEASED: "Inventory released",
};

interface AuditTimelineProps {
	events: AuditEvent[];
}

const DOT_CLASSES: Record<string, string> = {
	success: "bg-status-success-solid-light dark:bg-[#1EAD72]",
	danger: "bg-status-danger-solid-light dark:bg-[#E5484D]",
	warning: "bg-status-warning-solid-light dark:bg-[#D3932B]",
	info: "bg-brand-500 dark:bg-brand-400",
	muted: "bg-ink3-light dark:bg-ink3-dark",
};

export function AuditTimeline({ events }: AuditTimelineProps) {
	const [expandedId, setExpandedId] = useState<string | null>(null);

	if (events.length === 0) {
		return <p className="text-[13px] text-ink3-light dark:text-ink3-dark">No audit events found.</p>;
	}

	return (
		<ol className="relative space-y-6 pl-5">
			{events.map((event, index) => {
				const isExpanded = expandedId === event.id;
				const tone = EVENT_TONES[event.eventType] ?? "muted";
				const label = EVENT_LABELS[event.eventType] ?? event.eventType;

				return (
					<li key={event.id} className="relative">
						{index < events.length - 1 && (
							<span className="absolute -left-[11px] top-5 bottom-0 w-px bg-line-light dark:bg-line-dark" />
						)}
						<span
							className={`absolute -left-[15px] top-1.5 size-3.5 rounded-full border-2 border-card-light shadow-sm dark:border-card-dark ${DOT_CLASSES[tone]}`}
						/>
						<div className="flex items-start gap-3">
							<div className="min-w-0 flex-1">
								<div className="flex flex-wrap items-center gap-x-2.5 gap-y-0.5">
									<span className="text-[13px] font-semibold text-ink-light dark:text-ink-dark">{label}</span>
									<span className="text-[12px] text-ink3-light dark:text-ink3-dark">
										{new Date(event.createdAt).toLocaleString()}
									</span>
								</div>
								<button
									type="button"
									onClick={() => setExpandedId(isExpanded ? null : event.id)}
									className="mt-0.5 text-[12px] font-medium text-brand-600 transition-colors hover:text-brand-700 dark:text-brand-300 dark:hover:text-brand-200"
								>
									{isExpanded ? "Hide details" : "View details"}
								</button>
								{isExpanded && (
									<pre className="mt-2 max-h-60 overflow-x-auto overflow-y-auto rounded-lg border border-line-light bg-card2-light px-3 py-2.5 font-mono text-[12px] leading-relaxed text-ink2-light custom-scrollbar dark:border-line-dark dark:bg-card2-dark dark:text-ink2-dark">
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
					</li>
				);
			})}
		</ol>
	);
}
