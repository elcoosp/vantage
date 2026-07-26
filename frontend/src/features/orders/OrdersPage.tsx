import { OrdersTable } from "./OrdersTable";
import { useOrders } from "./useOrders";

export function OrdersPage() {
	const { data, isLoading, isError } = useOrders();

	return (
		<div className="space-y-4">
			<div className="flex justify-between items-center">
				<h2 className="text-2xl font-bold text-slate-100">Order Search</h2>
				{data && (
					<span className="text-sm text-slate-400">
						Showing {data.content.length.toLocaleString()} of {data.totalElements.toLocaleString()} orders
					</span>
				)}
			</div>

			{isError && (
				<div className="bg-red-900/50 border border-red-700 text-red-200 px-4 py-3 rounded">
					Failed to load orders. Please try again later.
				</div>
			)}

			<OrdersTable data={data?.content ?? []} isLoading={isLoading} />
		</div>
	);
}
