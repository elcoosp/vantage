import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Package, ShoppingCart, Store, TrendingUp, Wallet } from "lucide-react";
import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import {
	Card,
	CardHeader,
	EmptyState,
	Metric,
	PageHeader,
	StatusBadge,
	THead,
	Table,
	Td,
	Th,
	Tr,
	orderTone,
	statusLabel,
} from "../../components/ui";
import apiClient from "../../lib/api";

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

const STATUS_FILL: Record<string, string> = {
	CREATED: "#5B7CFA",
	CONFIRMED: "#6B5CE7",
	PAID: "#1EAD72",
	SHIPPED: "#D3932B",
	DELIVERED: "#2DBD7B",
	CANCELLED: "#E5484D",
};

export function Dashboard() {
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

	const recentOrders = allOrders?.slice(0, 6) ?? [];

	const fulfilledUnits = useMemo(() => {
		return (allOrders ?? [])
			.filter((o) => o.status === "PAID" || o.status === "DELIVERED")
			.reduce((sum, o) => sum + o.quantity, 0);
	}, [allOrders]);

	const statusCounts = useMemo(() => {
		const counts: Record<string, number> = {};
		for (const o of allOrders ?? []) {
			counts[o.status] = (counts[o.status] ?? 0) + 1;
		}
		return counts;
	}, [allOrders]);

	const dailyData = useMemo(() => {
		const daily: Record<string, Record<string, number>> = {};
		for (const o of allOrders ?? []) {
			const day = new Date(o.createdAt).toLocaleDateString("en-US", {
				month: "short",
				day: "numeric",
			});
			if (!daily[day]) daily[day] = {};
			daily[day][o.status] = (daily[day][o.status] ?? 0) + o.quantity;
		}
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
			.reverse()
			.slice(-14);
	}, [allOrders]);

	const lowStock = (inventory ?? []).filter((i) => i.quantity <= 10);

	const circuitHealthy = metrics?.paymentCircuitBreakerState === "UNKNOWN";

	const quickActions = [
		{ label: "Search orders", to: "/orders", Icon: ShoppingCart, desc: "Find and manage orders" },
		{ label: "Browse products", to: "/products", Icon: Package, desc: "Catalog and pricing" },
		{ label: "Stock levels", to: "/inventory", Icon: AlertTriangle, desc: "Inventory and reservations" },
		{ label: "Demand forecast", to: "/forecast", Icon: TrendingUp, desc: "AI predictions and insight" },
	];

	return (
		<div className="space-y-6 animate-fade-up">
			<PageHeader
				title="Overview"
				description="Your operating picture at a glance — status, volume, and what needs attention."
			/>

			{/* Metrics */}
			<div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
				<Metric
					label="Total vendors"
					value={metrics?.totalVendors ?? "—"}
					icon={<Store className="size-4" />}
					hint="Active merchant tenants"
				/>
				<Metric
					label="Total orders"
					value={metrics?.totalOrders ?? "—"}
					icon={<ShoppingCart className="size-4" />}
					hint="All-time order volume"
				/>
				<Metric
					label="Products"
					value={products?.length ?? "—"}
					icon={<Package className="size-4" />}
					hint="Items in your catalog"
				/>
				<Metric
					label="Low stock"
					value={lowStock.length}
					icon={<AlertTriangle className="size-4 text-status-warning-solid-light dark:text-[#F0C468]" />}
					hint="At or below reorder point"
				/>
				<Metric
					label="Payments"
					value={circuitHealthy ? "Healthy" : (metrics?.paymentCircuitBreakerState ?? "—")}
					hint={circuitHealthy ? "Gateway circuit closed" : "Circuit open"}
				/>
			</div>

			{/* Low-stock alert */}
			{lowStock.length > 0 && (
				<div className="anim-border rounded-xl border border-status-warning-soft-light bg-status-warning-soft-light p-4 dark:border-[#D3932B]/25 dark:bg-[#D3932B]/10">
					<div className="flex items-start gap-3">
						<span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-lg bg-status-warning-solid-light/15 text-status-warning-ink-light dark:bg-[#D3932B]/20 dark:text-[#F0C468]">
							<AlertTriangle className="size-4" />
						</span>
						<div className="min-w-0">
							<p className="text-sm font-semibold text-status-warning-ink-light dark:text-[#F0C468]">
								{lowStock.length} item{p(lowStock.length)} hitting the reorder point
							</p>
							<p className="mt-0.5 text-[13px] text-status-warning-ink-light/80 dark:text-[#F0C468]/80">
								Quantity ≤ 10. Restock before stockouts eat into orders.
							</p>
							<div className="mt-3 flex flex-wrap gap-1.5">
								{lowStock.map((item) => {
									const product = (products ?? []).find((p) => p.id === item.productId);
									const name = product?.name ?? `${item.productId.slice(0, 8)}…`;
									return (
										<span
											key={item.productId}
											className="rounded-md bg-status-warning-solid-light/10 px-2 py-0.5 font-mono text-[12px] text-status-warning-ink-light dark:bg-[#D3932B]/15 dark:text-[#F0C468]"
										>
											{name} · {item.quantity}
										</span>
									);
								})}
							</div>
						</div>
					</div>
				</div>
			)}

			{/* Two-column: trends + side panel */}
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
				<div className="space-y-4 lg:col-span-2">
					<Card>
						<CardHeader title="Order volume by day" subtitle="Quantity per order status, last 14 days" />
						<div className="px-3 pb-3 pt-1 text-ink3-light dark:text-ink3-dark">
							<ResponsiveContainer width="100%" height={280}>
								<BarChart data={dailyData} margin={{ top: 8, right: 12, left: 4, bottom: 4 }}>
									<CartesianGrid vertical={false} stroke="currentColor" strokeOpacity={0.08} />
									<XAxis
										dataKey="day"
										tick={{ fill: "currentColor", fontSize: 11 }}
										tickLine={false}
										axisLine={{ stroke: "currentColor", strokeOpacity: 0.2 }}
										minTickGap={12}
									/>
									<YAxis
										tick={{ fill: "currentColor", fontSize: 11 }}
										tickLine={false}
										axisLine={false}
										width={32}
										allowDecimals={false}
									/>
									<Tooltip cursor={{ fill: "currentColor", fillOpacity: 0.04 }} content={<DailyTooltip />} />
									<Legend
										iconType="circle"
										iconSize={8}
										wrapperStyle={{ fontSize: 12, paddingTop: 8 }}
										formatter={(value) => <span className="text-ink2-light dark:text-ink2-dark">{value}</span>}
									/>
									{Object.keys(STATUS_FILL).map((status) => (
										<Bar
											key={status}
											dataKey={status}
											stackId="a"
											fill={STATUS_FILL[status]}
											radius={[0, 0, 0, 0]}
											maxBarSize={26}
										/>
									))}
								</BarChart>
							</ResponsiveContainer>
						</div>
					</Card>

					<Card>
						<CardHeader
							title="Recent orders"
							subtitle="The latest activity across your storefront"
							action={
								<Link
									to="/orders"
									className="text-[13px] font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
								>
									View all
								</Link>
							}
						/>
						{ordersLoading ? (
							<div className="space-y-2 px-5 pb-5">
								{Array.from({ length: 4 }).map((_, i) => (
									// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton rows
									<div key={i} className="h-8 animate-pulse rounded-md bg-card2-light dark:bg-card2-dark" />
								))}
							</div>
						) : recentOrders.length === 0 ? (
							<EmptyState
								icon={<ShoppingCart className="size-5" />}
								title="No orders yet"
								description="Orders will appear here as they flow through your storefront."
							/>
						) : (
							<Table>
								<THead>
									<Th>Order</Th>
									<Th>Product</Th>
									<Th>Qty</Th>
									<Th>Status</Th>
									<Th>Created</Th>
								</THead>
								<tbody>
									{recentOrders.map((order) => (
										<Tr key={order.orderId}>
											<Td className="font-mono text-[12px] text-ink3-light dark:text-ink3-dark">
												{order.orderId.slice(0, 8)}
											</Td>
											<Td className="font-medium text-ink-light dark:text-ink-dark">{order.productName}</Td>
											<Td className="tabular-nums">{order.quantity}</Td>
											<Td>
												<StatusBadge tone={orderTone(order.status)}>
													{statusLabel[order.status] ?? order.status}
												</StatusBadge>
											</Td>
											<Td className="text-[13px] text-ink3-light dark:text-ink3-dark">
												{new Date(order.createdAt).toLocaleDateString()}
											</Td>
										</Tr>
									))}
								</tbody>
							</Table>
						)}
					</Card>
				</div>

				{/* Side panel */}
				<div className="space-y-4">
					{/* Pending statuses */}
					<Card>
						<CardHeader title="Orders by status" subtitle="Current distribution" />
						<div className="space-y-2.5 px-5 pb-5">
							{Object.entries(statusCounts).length === 0 ? (
								<p className="text-[13px] text-ink3-light dark:text-ink3-dark">No orders recorded yet.</p>
							) : (
								Object.entries(statusCounts)
									.sort((a, b) => b[1] - a[1])
									.map(([status, count]) => (
										<div
											key={status}
											className="flex items-center gap-3 rounded-lg px-2 py-1.5 transition-colors hover:bg-card2-light dark:hover:bg-card2-dark"
										>
											<span
												className="flex size-2 shrink-0 rounded-full"
												style={{ background: STATUS_FILL[status] ?? "#8A9099" }}
											/>
											<span className="flex-1 truncate text-[13px] text-ink2-light dark:text-ink2-dark">
												{statusLabel[status] ?? status}
											</span>
											<span className="text-[13px] font-semibold tabular-nums text-ink-light dark:text-ink-dark">
												{count}
											</span>
										</div>
									))
							)}
						</div>
					</Card>

					{/* Fulfilled units */}
					<Card>
						<CardHeader title="Fulfilled volume" subtitle="Units in paid or delivered orders" />
						<div className="px-5 pb-5">
							<div className="flex items-end gap-1.5">
								<Wallet className="mb-1 size-4 text-ink3-light dark:text-ink3-dark" />
								<span className="text-3xl font-semibold tabular-nums tracking-tight text-ink-light dark:text-ink-dark">
									{fulfilledUnits.toLocaleString()}
								</span>
							</div>
							<p className="mt-1.5 text-[13px] text-ink3-light dark:text-ink3-dark">
								Across all orders marked paid or delivered.
							</p>
						</div>
					</Card>

					{/* Quick actions */}
					<Card>
						<CardHeader title="Jump to" />
						<div className="space-y-1 px-3 pb-3">
							{quickActions.map((action) => (
								<Link
									key={action.to}
									to={action.to}
									className="group flex items-center gap-3 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-card2-light dark:hover:bg-card2-dark"
								>
									<span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600 transition-colors group-hover:bg-brand-100 dark:bg-brand-500/15 dark:text-brand-300 dark:group-hover:bg-brand-500/25">
										<action.Icon className="size-4" />
									</span>
									<span className="min-w-0 flex-1">
										<span className="block text-[13.5px] font-medium text-ink-light dark:text-ink-dark">
											{action.label}
										</span>
										<span className="block truncate text-[12.5px] text-ink3-light dark:text-ink3-dark">
											{action.desc}
										</span>
									</span>
								</Link>
							))}
						</div>
					</Card>
				</div>
			</div>
		</div>
	);
}

function p(n: number) {
	return n === 1 ? "" : "s";
}

/* Daily char tooltip */
interface DailyTooltipProps {
	active?: boolean;
	label?: string;
	payload?: Array<{ name: string; value: number; color: string }>;
}

function DailyTooltip({ active, label, payload }: DailyTooltipProps) {
	if (!active || !payload || payload.length === 0) return null;
	const total = payload.reduce((sum, entry) => sum + entry.value, 0);
	return (
		<div className="rounded-lg border border-line-light bg-card-light px-3 py-2 shadow-pop dark:border-line-dark dark:bg-card-dark">
			<p className="text-[12px] font-semibold text-ink-light dark:text-ink-dark">{label}</p>
			{total > 0 && (
				<p className="mt-0.5 text-[12px] tabular-nums text-ink3-light dark:text-ink3-dark">
					{total.toLocaleString()} total
				</p>
			)}
			<div className="mt-2 space-y-1">
				{payload
					.filter((entry) => entry.value > 0)
					.map((entry) => (
						<div key={entry.name} className="flex items-center gap-2 text-[12px]">
							<span className="size-1.5 rounded-full" style={{ background: entry.color }} />
							<span className="flex-1 text-ink2-light dark:text-ink2-dark">
								{statusLabel[entry.name] ?? entry.name}
							</span>
							<span className="tabular-nums text-ink-light dark:text-ink-dark">{entry.value}</span>
						</div>
					))}
			</div>
		</div>
	);
}
