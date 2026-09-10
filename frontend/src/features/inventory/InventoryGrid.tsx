import { Package, TrendingDown } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../../lib/api";
import { InventoryEditForm } from "./InventoryEditForm";

interface InventoryItem {
	productId: string;
	quantity: number;
	version: number;
}

interface ProductSummary {
	id: string;
	name: string;
}

async function fetchInventory(): Promise<InventoryItem[]> {
	const res = await apiClient.get<InventoryItem[]>("/inventory");
	return res.data;
}

async function fetchProducts(): Promise<ProductSummary[]> {
	const res = await apiClient.get<ProductSummary[]>("/products");
	return res.data;
}

async function updateInventory(productId: string, quantity: number, version: number) {
	await apiClient.put(
		`/inventory/${productId}`,
		{ quantity },
		{
			headers: { "If-Match": String(version) },
		},
	);
}

export function InventoryGrid() {
	const queryClient = useQueryClient();

	const { data: inventory, isLoading, error } = useQuery({
		queryKey: ["inventory"],
		queryFn: fetchInventory,
		retry: false,
	});

	const { data: products } = useQuery({
		queryKey: ["products-for-inventory"],
		queryFn: fetchProducts,
		retry: false,
		staleTime: 60000,
	});

	const mutation = useMutation({
		mutationFn: ({
			productId,
			quantity,
			version,
		}: { productId: string; quantity: number; version: number }) =>
			updateInventory(productId, quantity, version),
		onError: () => {
			toast.error("Conflict: Another user modified this item. Please refresh.");
		},
		onSettled: () => {
			queryClient.invalidateQueries({ queryKey: ["inventory"] });
		},
	});

	const [editingId, setEditingId] = useState<string | null>(null);

	const lowStock = (inventory ?? []).filter((i) => i.quantity <= 10);

	if (isLoading) return <div className="p-4">Loading inventory...</div>;
	if (error) return <div className="p-4 text-red-600">Failed to load inventory</div>;

	if (!inventory || inventory.length === 0) {
		return (
			<div className="text-center py-12 text-slate-500 dark:text-slate-400">
				<div className="mb-2 w-fit mx-auto">
					<Package className="h-8 w-8 text-slate-400" />
				</div>
				<p>No inventory data. Create products and they will appear here.</p>
			</div>
		);
	}

	return (
		<div className="space-y-6">
			{lowStock.length > 0 && (
				<div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl p-4">
					<div className="flex items-center gap-2 text-amber-800 dark:text-amber-200 font-medium">
						<TrendingDown className="h-5 w-5" />
						{lowStock.length} item(s) below low-stock threshold (≤10)
					</div>
				</div>
			)}

			<div className="overflow-x-auto">
				<table className="min-w-full bg-white dark:bg-slate-800 shadow rounded-lg">
					<thead>
						<tr className="border-b border-slate-200 dark:border-slate-700">
							<th className="px-4 py-2 text-left">Product</th>
							<th className="px-4 py-2 text-left">Product ID</th>
							<th className="px-4 py-2 text-left">Quantity</th>
							<th className="px-4 py-2 text-left">Version</th>
							<th className="px-4 py-2 text-left">Actions</th>
						</tr>
					</thead>
					<tbody>
						{inventory.map((item) => {
							const product = products?.find((p) => p.id === item.productId);
							const name = product?.name ?? item.productId.slice(0, 8) + "...";
							const isLow = item.quantity <= 10;
							return (
								<tr
									key={item.productId}
									className="border-b border-slate-100 dark:border-slate-700"
								>
									<td className="px-4 py-2">
										<div className="font-medium text-slate-900 dark:text-slate-100">
											{name}
										</div>
									</td>
									<td className="px-4 py-2 font-mono text-xs text-slate-600 dark:text-slate-300">
										{item.productId.slice(0, 8)}...
									</td>
									<td className="px-4 py-2">
										<span
										 className={`font-semibold ${
											 isLow
												? "text-amber-600 dark:text-amber-400"
												: "text-slate-900 dark:text-slate-100"
										 }`}
										>
											{item.quantity}
										</span>
									</td>
									<td className="px-4 py-2 text-slate-500 dark:text-slate-400">
										{item.version}
									</td>
									<td className="px-4 py-2">
										{editingId === item.productId ? (
											<InventoryEditForm
												currentQuantity={item.quantity}
												onSubmit={(qty) => {
													mutation.mutate({
														productId: item.productId,
														quantity: qty,
														version: item.version,
													});
													setEditingId(null);
												}}
												onCancel={() => setEditingId(null)}
												isPending={mutation.isPending}
											/>
										) : (
											<button
												type="button"
												onClick={() => setEditingId(item.productId)}
												className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
											>
												Edit Quantity
											</button>
										)}
									</td>
								</tr>
							);
						})}
					</tbody>
				</table>
			</div>
		</div>
	);
}
