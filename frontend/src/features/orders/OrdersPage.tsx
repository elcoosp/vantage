import { Search } from "lucide-react";
import { useState } from "react";
import { EmptyState, Input, PageHeader } from "../../components/ui";
import { OrdersTable } from "./OrdersTable";
import { useOrders } from "./useOrders";

export function OrdersPage() {
	const { data, isLoading, isError } = useOrders();
	const [searchTerm, setSearchTerm] = useState("");

	const filteredContent =
		data?.content.filter(
			(order) =>
				order.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
				order.status.toLowerCase().includes(searchTerm.toLowerCase()) ||
				order.orderId.toLowerCase().includes(searchTerm.toLowerCase()),
		) ?? [];

	return (
		<div className="space-y-5 animate-fade-up">
			<PageHeader
				title="Orders"
				description="Search and inspect every order across your storefront."
				actions={
					data && (
						<span className="rounded-md bg-card2-light px-2.5 py-1 font-mono text-[12px] text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
							{filteredContent.length.toLocaleString()} / {data.totalElements.toLocaleString()}
						</span>
					)
				}
			/>

			<div className="relative max-w-md">
				<Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink3-light dark:text-ink3-dark" />
				<Input
					type="text"
					placeholder="Search by product, status, or order ID…"
					value={searchTerm}
					onChange={(e) => setSearchTerm(e.target.value)}
					className="pl-9"
				/>
			</div>

			{isError && (
				<div className="rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
					Failed to load orders. Please try again later.
				</div>
			)}

			{!isLoading && (!data?.content || data.content.length === 0) ? (
				<div className="rounded-xl border border-line-light bg-card-light p-4 dark:border-line-dark dark:bg-card-dark">
					<EmptyState
						icon={<Search className="size-5" />}
						title="No orders found"
						description="Orders will appear here once your storefront starts receiving requests."
					/>
				</div>
			) : (
				<OrdersTable data={filteredContent} isLoading={isLoading} />
			)}
		</div>
	);
}
