import { Search } from "lucide-react";
import { useState } from "react";
import { OrdersTable } from "./OrdersTable";
import { useOrders } from "./useOrders";

export function OrdersPage() {
	const { data, isLoading, isError } = useOrders();
	const [searchTerm, setSearchTerm] = useState("");

	const filteredContent = data?.content.filter(
		(order) =>
			order.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
			order.status.toLowerCase().includes(searchTerm.toLowerCase()) ||
			order.orderId.toLowerCase().includes(searchTerm.toLowerCase()),
	) ?? [];

	return (
		<div className="space-y-4">
			<div className="flex justify-between items-center">
				<h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
					Order Search
				</h2>
				{data && (
					<span className="text-sm text-slate-500 dark:text-slate-400">
						Showing {filteredContent.length.toLocaleString()} of{" "}
						{data.totalElements.toLocaleString()} orders
					</span>
				)}
			</div>

			<div className="relative">
				<Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
				<input
					type="text"
					placeholder="Search by product, status, or order ID..."
					value={searchTerm}
					onChange={(e) => setSearchTerm(e.target.value)}
					className="w-full pl-10 pr-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
				/>
			</div>

			{isError && (
				<div className="bg-red-900/20 border border-red-700 text-red-200 px-4 py-3 rounded">
					Failed to load orders. Please try again later.
				</div>
			)}

			{!isLoading && (!data?.content || data.content.length === 0) ? (
				<div className="text-center py-12 text-slate-500 dark:text-slate-400">
					<div className="mb-2 w-fit mx-auto">
						<Search className="h-8 w-8 text-slate-400" />
					</div>
					<p>No orders found. Create your first order to get started.</p>
				</div>
			) : (
				<OrdersTable data={filteredContent} isLoading={isLoading} />
			)}
		</div>
	);
}
