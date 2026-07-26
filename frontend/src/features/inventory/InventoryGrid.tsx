import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useOptimistic, useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../../lib/api";
import { InventoryEditForm } from "./InventoryEditForm";

interface InventoryItem {
	productId: string;
	quantity: number;
	version: number;
}

// TODO: Replace with actual API call once GET /api/v1/inventory is implemented.
// Currently using mock data for demonstration.
const MOCK_INVENTORY: InventoryItem[] = [
	{ productId: "11111111-1111-1111-1111-111111111111", quantity: 10, version: 1 },
	{ productId: "22222222-2222-2222-2222-222222222222", quantity: 5, version: 2 },
	{ productId: "33333333-3333-3333-3333-333333333333", quantity: 20, version: 3 },
];

async function fetchInventory(): Promise<InventoryItem[]> {
	// Uncomment this when backend endpoint is ready:
	// const res = await apiClient.get("/inventory");
	// return res.data;
	return MOCK_INVENTORY;
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
	const { data, isLoading, error } = useQuery({
		queryKey: ["inventory"],
		queryFn: fetchInventory,
	});

	const [optimisticItems, setOptimisticItems] = useOptimistic(data || [], (state, newItem: InventoryItem) =>
		state.map((item) => (item.productId === newItem.productId ? newItem : item)),
	);

	const mutation = useMutation({
		mutationFn: ({ productId, quantity, version }: { productId: string; quantity: number; version: number }) =>
			updateInventory(productId, quantity, version),
		onMutate: async (variables) => {
			await queryClient.cancelQueries({ queryKey: ["inventory"] });
			const previous = queryClient.getQueryData<InventoryItem[]>(["inventory"]);
			setOptimisticItems({
				productId: variables.productId,
				quantity: variables.quantity,
				version: variables.version + 1,
			});
			return { previous };
		},
		onError: (_err, _variables, context) => {
			toast.error("Conflict: Another user modified this item. Please refresh.");
			if (context?.previous) {
				queryClient.setQueryData(["inventory"], context.previous);
			}
		},
		onSettled: () => {
			queryClient.invalidateQueries({ queryKey: ["inventory"] });
		},
	});

	const [editingId, setEditingId] = useState<string | null>(null);

	if (isLoading) return <div className="p-4">Loading inventory...</div>;
	if (error) return <div className="p-4 text-red-600">Failed to load inventory</div>;

	return (
		<div className="overflow-x-auto">
			<table className="min-w-full bg-white dark:bg-gray-800 shadow rounded-lg">
				<thead>
					<tr className="border-b border-gray-200 dark:border-gray-700">
						<th className="px-4 py-2 text-left">Product</th>
						<th className="px-4 py-2 text-left">Quantity</th>
						<th className="px-4 py-2 text-left">Version</th>
						<th className="px-4 py-2 text-left">Actions</th>
					</tr>
				</thead>
				<tbody>
					{optimisticItems.map((item) => (
						<tr key={item.productId} className="border-b border-gray-100 dark:border-gray-700">
							<td className="px-4 py-2">{item.productId}</td>
							<td className="px-4 py-2">{item.quantity}</td>
							<td className="px-4 py-2">{item.version}</td>
							<td className="px-4 py-2">
								{editingId === item.productId ? (
									<InventoryEditForm
										currentQuantity={item.quantity}
										onSubmit={(qty) => {
											mutation.mutate({ productId: item.productId, quantity: qty, version: item.version });
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
					))}
				</tbody>
			</table>
		</div>
	);
}
