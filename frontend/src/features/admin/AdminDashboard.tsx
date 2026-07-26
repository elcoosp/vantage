import type React from "react";
import { useAdminMetrics } from "./useAdminMetrics";
import { useChaosMonkey } from "./useChaosMonkey";

export function AdminDashboard() {
	const { data: metrics, isLoading: metricsLoading, error: metricsError } = useAdminMetrics();
	const { isEnabled, isLoading: chaosLoading, toggle, isToggling } = useChaosMonkey();

	const handleToggle = (event: React.ChangeEvent<HTMLInputElement>) => {
		toggle(event.target.checked);
	};

	if (metricsLoading || chaosLoading) {
		return <div className="p-4">Loading admin dashboard...</div>;
	}

	if (metricsError) {
		return <div className="p-4 text-red-600">Failed to load metrics</div>;
	}

	return (
		<div className="p-4 space-y-6">
			<h2 className="text-2xl font-bold">Platform Admin Dashboard</h2>

			{/* Metrics Cards */}
			<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
				<div className="bg-white shadow rounded-lg p-4">
					<div className="text-sm text-gray-500">Total Vendors</div>
					<div className="text-2xl font-semibold">{metrics?.totalVendors ?? 0}</div>
				</div>
				<div className="bg-white shadow rounded-lg p-4">
					<div className="text-sm text-gray-500">Total Orders</div>
					<div className="text-2xl font-semibold">{metrics?.totalOrders ?? 0}</div>
				</div>
				<div className="bg-white shadow rounded-lg p-4">
					<div className="text-sm text-gray-500">Circuit Breaker</div>
					<div className="text-2xl font-semibold">{metrics?.paymentCircuitBreakerState ?? "UNKNOWN"}</div>
				</div>
			</div>

			{/* Chaos Monkey Control */}
			<div className="bg-white shadow rounded-lg p-4 border-l-4 border-yellow-400">
				<div className="flex items-center justify-between">
					<div>
						<h3 className="text-lg font-medium">Chaos Monkey</h3>
						<p className="text-sm text-gray-500">Simulate payment gateway failures to test resilience</p>
					</div>
					<div className="flex items-center space-x-2">
						<span className={`text-sm ${isEnabled ? "text-red-600 font-semibold" : "text-gray-500"}`}>
							{isEnabled ? "ACTIVE" : "Inactive"}
						</span>
						<label className="relative inline-flex items-center cursor-pointer">
							<input
								type="checkbox"
								className="sr-only peer"
								checked={isEnabled}
								onChange={handleToggle}
								disabled={isToggling}
							/>
							<div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-red-600" />
						</label>
					</div>
				</div>
				{isEnabled && (
					<div className="mt-2 text-sm text-red-600 bg-red-50 p-2 rounded">
						⚠️ Chaos Monkey is active – payment failures will be simulated.
					</div>
				)}
			</div>
		</div>
	);
}
