import { useQuery } from "@tanstack/react-query";
import { BarChart3, Package, ShoppingCart, Shield, TrendingUp, Wallet } from "lucide-react";
import { useMemo } from "react";
import {
	BarChart,
	Bar,
	XAxis,
	YAxis,
	Tooltip,
	Legend,
	ResponsiveContainer,
} from "recharts";
import { Link } from "react-router-dom";
import apiClient from "../../lib/api";
import { useAuthStore } from "../../store/authStore";

interface SystemMetrics {
	totalVendors: number;
	totalOrders: number;
	paymentCircuitBreakerState: string;
}

interface OrderSummary {
	orderId: string;
	productName: string;
	status: string;
	quantity: number;
	createdAt: string;
}

interface ProductSummary {
	id: string;
	name: string;
	price: number;
}

interface InventorySummary {
	productId: string;
	quantity: number;
}

async function fetchMetrics(): Promise<SystemMetrics> {
	const res = await apiClient.get<SystemMetrics>("/admin/metrics");
	return res.data;
}

async function fetchRecentOrders(): Promise<OrderSummary[]> {
	const res = await apiClient.get<{ content: OrderSummary[] }>("/orders/search", {
		params: { size: 1000 },
	});
	return res.data.content;
}

async function fetchProducts(): Promise<ProductSummary[]> {
	const res = await apiClient.get<ProductSummary[]>("/products");
	return res.data;
}

async function fetchInventory(): Promise<InventorySummary[]> {
	const res = await apiClient.get<InventorySummary[]>("/inventory");
	return res.data;
}

const statusColors: Record<string, { bg: string; text: string; label: string }> = {
	CREATED: { bg: "bg-blue-100 dark:bg-blue-900/30", text: "text-blue-800 dark:text-blue-200", label: "Created" },
	CONFIRMED: { bg: "bg-indigo-100 dark:bg-indigo-900/30", text: "text-indigo-800 dark:text-indigo-200", label: "Confirmed" },
	PAID: { bg: "bg-green-100 dark:bg-green-900/30", text: "text-green-800 dark:text-green-200", label: "Paid" },
	CANCELLED: { bg: "bg-red-100 dark:bg-red-900/30", text: "text-red-800 dark:text-red-200", label: "Cancelled" },
	SHIPPED: { bg: "bg-amber-100 dark:bg-amber-900/30", text: "text-amber-800 dark:text-amber-200", label: "Shipped" },
	DELIVERED: { bg: "bg-emerald-100 dark:bg-emerald-900/30", text: "text-emerald-800 dark:text-emerald-200", label: "Delivered" },
};

const statusBarColors: Record<string, string> = {
	CREATED: "bg-blue-500",
	CONFIRMED: "bg-indigo-500",
	PAID: "bg-green-500",
	CANCELLED: "bg-red-500",
	SHIPPED: "bg-amber-500",
	DELIVERED: "bg-emerald-500",
};

const statusLabels: Record<string, string> = {
	CREATED: "Created",
	CONFIRMED: "Confirmed",
	PAID: "Paid",
	CANCELLED: "Cancelled",
	SHIPPED: "Shipped",
	DELIVERED: "Delivered",
};

export function Dashboard() {
	const tenantId = useAuthStore((s) => s.tenantId);

	const { data: metrics } = useQuery({
		queryKey: ["dashboard-metrics"],
		queryFn: fetchMetrics,
		retry: false,
	});

	const { data: allOrders, isLoading: ordersLoading } = useQuery({
		queryKey: ["dashboard-orders"],
		queryFn: fetchRecentOrders,
		retry: false,
	});

	const { data: products } = useQuery({
		queryKey: ["dashboard-products"],
		queryFn: fetchProducts,
		retry: false,
	});

	const { data: inventory } = useQuery({
		queryKey: ["dashboard-inventory"],
		queryFn: fetchInventory,
		retry: false,
	});

	const recentOrders = allOrders?.slice(0, 5) ?? [];

	const totalRevenue = useMemo(() => {
		return (allOrders ?? [])
			.filter((o) => o.status === "PAID" || o.status === "DELIVERED")
			.reduce((sum, o) => sum + o.quantity, 0);
	}, [allOrders]);

	const statusCounts = useMemo(() => {
		const counts: Record<string, number> = {};
		(allOrders ?? []).forEach((o) => {
			counts[o.status] = (counts[o.status] ?? 0) + 1;
		});
		return counts;
	}, [allOrders]);

	const dailyData = useMemo(() => {
		const daily: Record<string, Record<string, number>> = {};
		(allOrders ?? []).forEach((o) => {
			const day = new Date(o.createdAt).toLocaleDateString("en-US", {
				month: "short",
				day: "numeric",
			});
			if (!daily[day]) daily[day] = {};
			daily[day][o.status] = (daily[day][o.status] ?? 0) + o.quantity;
		});
		return Object.entries(daily)
			.map(([day, statuses]) => ({
				day,
				CREATED: statuses.CREATED ?? 0,
				CONFIRMED: statuses.CONFIRMED ?? 0,
				PAID: statuses.PAID ?? 0,
				SHIPPED: statuses.SHIPPED ?? 0,
				DELIVERED: statuses.DELIVERED ?? 0,
				CANCELLED: statuses.CANCELLED ?? 0,
			}))
			.reverse();
	}, [allOrders]);

	const lowStock = (inventory ?? []).filter((i) => i.quantity <= 10);

	const quickActions = [
		{ label: "View Orders", to: "/orders", Icon: ShoppingCart, desc: "Search and manage orders" },
		{ label: "Manage Products", to: "/products", Icon: Package, desc: "Browse and add products" },
		{ label: "Check Inventory", to: "/inventory", Icon: BarChart3, desc: "Stock levels and reservations" },
		{ label: "Analytics", to: "/forecast", Icon: TrendingUp, desc: "Demand forecasts and insights" },
	];

	return (
		<div className="space-y-6">
			<div className="flex justify-between items-center">
				<div>
					<h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						Platform Dashboard
					</h1>
					<p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
						Tenant ID: {tenantId?.slice(0, 8)}...
					</p>
				</div>
			</div>

			{/* Metrics Cards */}
			<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
				<div className="bg-white dark:bg-slate-800 rounded-xl shadow p-6 border border-slate-200 dark:border-slate-700">
					<div className="flex items-center justify-between mb-2">
						<span className="text-sm text-slate-500 dark:text-slate-400">Total Vendors</span>
						<BarChart3 className="h-5 w-5 text-slate-600 dark:text-slate-300" />
					</div>
					<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						{metrics?.totalVendors ?? "—"}
					</div>
				</div>

				<div className="bg-white dark:bg-slate-800 rounded-xl shadow p-6 border border-slate-200 dark:border-slate-700">
					<div className="flex items-center justify-between mb-2">
						<span className="text-sm text-slate-500 dark:text-slate-400">Total Orders</span>
						<ShoppingCart className="h-5 w-5 text-slate-600 dark:text-slate-300" />
					</div>
					<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						{metrics?.totalOrders ?? "—"}
					</div>
				</div>

				<div className="bg-white dark:bg-slate-800 rounded-xl shadow p-6 border border-slate-200 dark:border-slate-700">
					<div className="flex items-center justify-between mb-2">
						<span className="text-sm text-slate-500 dark:text-slate-400">Products</span>
						<Package className="h-5 w-5 text-slate-600 dark:text-slate-300" />
					</div>
					<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						{products?.length ?? "—"}
					</div>
				</div>

				<div className="bg-white dark:bg-slate-800 rounded-xl shadow p-6 border border-slate-200 dark:border-slate-700">
					<div className="flex items-center justify-between mb-2">
						<span className="text-sm text-slate-500 dark:text-slate-400">Low Stock</span>
						<Package className="h-5 w-5 text-amber-600" />
					</div>
					<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						{lowStock.length}
					</div>
				</div>

				<div className="bg-white dark:bg-slate-800 rounded-xl shadow p-6 border border-slate-200 dark:border-slate-700">
					<div className="flex items-center justify-between mb-2">
						<span className="text-sm text-slate-500 dark:text-slate-400">Circuit Breaker</span>
						<Shield className="h-5 w-5 text-slate-600 dark:text-slate-300" />
					</div>
					<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
						{metrics?.paymentCircuitBreakerState === "UNKNOWN"
							? "Healthy"
							: metrics?.paymentCircuitBreakerState ?? "—"}
					</div>
				</div>
			</div>

			{lowStock.length > 0 && (
				<div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl p-4">
					<div className="flex items-center gap-2 text-amber-800 dark:text-amber-200 font-medium">
						<Package className="h-5 w-5" />
						{lowStock.length} item(s) below low-stock threshold (≤10)
					</div>
					<div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2 text-sm mt-2">
						{lowStock.map((item) => {
							const product = (products ?? []).find((p) => p.id === item.productId);
							const name = product?.name ?? item.productId.slice(0, 8) + "...";
							return (
								<span
									key={item.productId}
									className="bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-200 px-3 py-1 rounded"
								>
									{name}: {item.quantity} left
								</span>
							);
						})}
					</div>
				</div>
			)}

			{/* Quick Actions */}
			<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
				{quickActions.map((action) => (
					<Link
						key={action.label}
						to={action.to}
						className="bg-white dark:bg-slate-800 rounded-xl shadow p-5 border border-slate-200 dark:border-slate-700 hover:shadow-md transition-shadow text-center block"
					>
						<div className="mb-2 w-fit mx-auto">
							<action.Icon className="h-6 w-6 text-slate-600 dark:text-slate-300" />
						</div>
						<div className="font-medium text-slate-900 dark:text-slate-100">{action.label}</div>
						<div className="text-sm text-slate-500 dark:text-slate-400 mt-1">
							{action.desc}
						</div>
					</Link>
				))}
			</div>

			{/* Order Status Breakdown */}
			{Object.keys(statusCounts).length > 0 && (
				<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-6">
					<h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">
						Order Status Breakdown
					</h2>
					<div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
						{Object.entries(statusCounts).map(([status, count]) => (
							<div key={status} className="flex items-center gap-3">
								<div
									className={`w-3 h-3 rounded-full ${
										statusBarColors[status] ?? "bg-slate-500"
									}`}
								/>
								<span className="text-sm text-slate-700 dark:text-slate-300">
									{statusLabels[status] ?? status}: {count}
								</span>
							</div>
						))}
					</div>
				</div>
			)}

			{/* Revenue Chart */}
			{dailyData.length > 0 && (
				<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-6">
					<h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">
						Orders by Day
					</h2>
					<ResponsiveContainer width="100%" height={320}>
						<BarChart data={dailyData} margin={{ top: 20, right: 30, left: 0, bottom: 50 }}>
							<XAxis
								dataKey="day"
								tick={{ fill: "currentColor", fontSize: 12 }}
								tickLine={false}
								axisLine={false}
							/>
							<YAxis
								tick={{ fill: "currentColor", fontSize: 12 }}
								tickLine={false}
								axisLine={false}
							/>
							<Tooltip
								contentStyle={{
									backgroundColor: "rgba(248, 250, 252, 0.95)",
									border: "1px solid #e2e8f0",
								}}
								position={{ y: -80 }}
							/>
							<Legend layout="horizontal" verticalAlign="bottom" height={40} iconSize={10} />
							<Bar dataKey="CREATED" stackId="a" fill="#3b82f6" />
							<Bar dataKey="CONFIRMED" stackId="a" fill="#8b5cf6" />
							<Bar dataKey="PAID" stackId="a" fill="#22c55e" />
							<Bar dataKey="SHIPPED" stackId="a" fill="#f59e0b" />
							<Bar dataKey="DELIVERED" stackId="a" fill="#10b981" />
							<Bar dataKey="CANCELLED" stackId="a" fill="#ef4444" />
						</BarChart>
					</ResponsiveContainer>
				</div>
			)}

			{/* Revenue Summary */}
			<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-6">
				<div className="flex items-center justify-between mb-4">
					<h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
						Revenue Summary
					</h2>
					<Wallet className="h-5 w-5 text-slate-600 dark:text-slate-300" />
				</div>
				<div className="text-3xl font-bold text-slate-900 dark:text-slate-100">
					${totalRevenue.toFixed(2)}
				</div>
				<p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
					Total units in paid/delivered orders
				</p>
			</div>

			{/* Recent Orders */}
			<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700">
				<div className="px-6 py-4 border-b border-slate-200 dark:border-slate-700 flex justify-between items-center">
					<h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
						Recent Orders
					</h2>
					<Link
						to="/orders"
						className="text-sm text-blue-600 hover:text-blue-500 dark:text-blue-400"
					>
						View all
					</Link>
				</div>
				<div className="overflow-x-auto">
					<table className="w-full">
						<thead className="bg-slate-50 dark:bg-slate-900/50">
							<tr>
								<th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
									ID
								</th>
								<th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
									Product
								</th>
								<th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
									Qty
								</th>
								<th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
									Status
								</th>
								<th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
									Created
								</th>
							</tr>
						</thead>
						<tbody>
							{ordersLoading ? (
								<tr>
									<td colSpan={5} className="px-4 py-4 text-center text-slate-500">
										Loading...
									</td>
								</tr>
							) : recentOrders.length === 0 ? (
								<tr>
									<td colSpan={5} className="px-4 py-8 text-center text-slate-500">
										No orders found. Create your first order to get started.
									</td>
								</tr>
							) : (
								recentOrders.map((order) => {
									const sc = statusColors[order.status] ?? { bg: "bg-slate-100", text: "text-slate-800", label: order.status };
									return (
										<tr
											key={order.orderId}
											className="border-b border-slate-200 dark:border-slate-700"
										>
											<td className="px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
												{order.orderId.slice(0, 8)}...
											</td>
											<td className="px-4 py-3 text-slate-900 dark:text-slate-100">
												{order.productName}
											</td>
											<td className="px-4 py-3 text-slate-600 dark:text-slate-300">
												{order.quantity}
											</td>
											<td className="px-4 py-3">
												<span
													className={`px-2 py-1 text-xs font-semibold rounded ${sc.bg} ${sc.text}`}
												>
													{sc.label ?? order.status}
												</span>
											</td>
											<td className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
												{new Date(order.createdAt).toLocaleString()}
											</td>
										</tr>
									);
								})
							)}
						</tbody>
					</table>
				</div>
			</div>
		</div>
	);
}
