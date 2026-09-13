import { AlertTriangle, ShieldAlert } from "lucide-react";
import { Card, CardHeader, Metric, PageHeader, StatusBadge } from "../../components/ui";
import { useAdminMetrics } from "./useAdminMetrics";
import { useChaosMonkey } from "./useChaosMonkey";

export function AdminDashboard() {
	const { data: metrics, isLoading: metricsLoading, error: metricsError } = useAdminMetrics();
	const { isEnabled, isLoading: chaosLoading, toggle, isToggling } = useChaosMonkey();

	if (metricsLoading || chaosLoading) {
		return (
			<div className="space-y-5 animate-fade-up">
				<PageHeader title="Admin" description="Platform-wide operations." />
				<div className="grid gap-4 sm:grid-cols-3">
					{Array.from({ length: 3 }).map((_, i) => (
						// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton cards
						<div key={i} className="h-28 animate-pulse rounded-xl bg-card-light dark:bg-card-dark" />
					))}
				</div>
			</div>
		);
	}

	if (metricsError) {
		return (
			<div className="rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
				Failed to load admin metrics.
			</div>
		);
	}

	return (
		<div className="space-y-6 animate-fade-up">
			<PageHeader title="Admin" description="Platform-wide health, vendors, and resilience tooling." />

			<div className="grid gap-4 sm:grid-cols-3">
				<Metric label="Total vendors" value={(metrics?.totalVendors ?? 0).toLocaleString()} />
				<Metric label="Total orders" value={(metrics?.totalOrders ?? 0).toLocaleString()} />
				<Metric
					label="Payment circuit breaker"
					value={metrics?.paymentCircuitBreakerState ?? "UNKNOWN"}
					hint={
						metrics?.paymentCircuitBreakerState === "OPEN" ? (
							<span className="inline-flex items-center gap-1 text-status-danger-ink-light dark:text-[#FF9A9D]">
								<AlertTriangle className="size-3" /> Payments temporarily failing
							</span>
						) : undefined
					}
				/>
			</div>

			<Card>
				<CardHeader title="Chaos Monkey" subtitle="Simulate payment gateway failures to test resilience." />
				<div className="flex flex-wrap items-center justify-between gap-4 px-5 pb-5">
					<div className="flex items-center gap-3">
						<span
							className={`flex size-9 items-center justify-center rounded-lg ${
								isEnabled
									? "bg-status-danger-soft-light text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]"
									: "bg-card2-light text-ink3-light dark:bg-card2-dark dark:text-ink3-dark"
							}`}
						>
							<ShieldAlert className="size-4" />
						</span>
						<StatusBadge tone={isEnabled ? "danger" : "info"}>{isEnabled ? "Active" : "Inactive"}</StatusBadge>
					</div>
					<div className="inline-flex cursor-pointer items-center gap-3">
						<span
							className={`text-[13px] font-medium ${isEnabled ? "text-status-danger-ink-light dark:text-[#FF9A9D]" : "text-ink3-light dark:text-ink3-dark"}`}
						>
							{isEnabled ? "Armed" : "Disarmed"}
						</span>
						<button
							type="button"
							role="switch"
							aria-checked={isEnabled}
							disabled={isToggling}
							onClick={() => toggle(!isEnabled)}
							className={`relative h-6 w-11 rounded-full transition-colors ${
								isEnabled ? "bg-status-danger-solid-light dark:bg-[#E5484D]" : "bg-card2-light dark:bg-card2-dark"
							} ${isToggling ? "opacity-50" : ""}`}
						>
							<span
								className={`absolute top-0.5 left-0.5 size-5 rounded-full bg-white shadow-sm transition-transform ${
									isEnabled ? "translate-x-5" : "translate-x-0"
								}`}
							/>
						</button>
					</div>
				</div>
				{isEnabled && (
					<div className="mx-5 mb-5 flex items-start gap-2 rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-3 py-2.5 text-[13px] text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
						<AlertTriangle className="mt-0.5 size-4 shrink-0" />
						Chaos Monkey is armed — payment failures will be simulated. Watch the circuit breaker trip and recover.
					</div>
				)}
			</Card>
		</div>
	);
}
