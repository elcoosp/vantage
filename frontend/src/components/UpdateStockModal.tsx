import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../lib/api";
import { useUIStore } from "../store/uiStore";

interface InventoryItem {
	productId: string;
	quantity: number;
	version: number;
}

async function fetchInventory(): Promise<InventoryItem[]> {
	const res = await apiClient.get("/inventory");
	return res.data;
}

export function UpdateStockModal() {
	const { updateStockModalOpen, closeUpdateStockModal } = useUIStore();
	const [selectedProduct, setSelectedProduct] = useState<string>("");
	const [newQuantity, setNewQuantity] = useState<number>(0);
	const queryClient = useQueryClient();

	const { data, isLoading } = useQuery({
		queryKey: ["inventory"],
		queryFn: fetchInventory,
		enabled: updateStockModalOpen,
	});

	const mutation = useMutation({
		mutationFn: async () => {
			const item = data?.find((i) => i.productId === selectedProduct);
			if (!item) throw new Error("Product not found");
			await apiClient.put(
				`/inventory/${selectedProduct}`,
				{ quantity: newQuantity },
				{
					headers: { "If-Match": String(item.version) },
				},
			);
		},
		onSuccess: () => {
			toast.success("Stock updated");
			queryClient.invalidateQueries({ queryKey: ["inventory"] });
			closeUpdateStockModal();
			setSelectedProduct("");
			setNewQuantity(0);
		},
		onError: () => {
			toast.error("Failed to update stock. Conflict or error.");
		},
	});

	if (!updateStockModalOpen) return null;

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		mutation.mutate();
	};

	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
			<div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
				<h2 className="text-xl font-bold mb-4">Update Stock</h2>
				<form onSubmit={handleSubmit}>
					<div className="mb-3">
						<label htmlFor="update-product" className="block text-sm font-medium mb-1">
							Product
						</label>
						<select
							value={selectedProduct}
							onChange={(e) => {
								setSelectedProduct(e.target.value);
								const item = data?.find((i) => i.productId === e.target.value);
								if (item) setNewQuantity(item.quantity);
							}}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							required
							disabled={isLoading}
						>
							<option value="">Select a product</option>
							{data?.map((item) => (
								<option key={item.productId} value={item.productId}>
									{item.productId} (current: {item.quantity})
								</option>
							))}
						</select>
					</div>
					<div className="mb-3">
						<label htmlFor="update-quantity" className="block text-sm font-medium mb-1">
							New Quantity
						</label>
						<input
							type="number"
							value={newQuantity}
							onChange={(e) => setNewQuantity(Number(e.target.value))}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							required
						/>
					</div>
					<div className="flex justify-end gap-2">
						<button
							type="button"
							onClick={closeUpdateStockModal}
							className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
							disabled={mutation.isPending}
						>
							Cancel
						</button>
						<button
							type="submit"
							className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
							disabled={mutation.isPending || !selectedProduct}
						>
							{mutation.isPending ? "Updating..." : "Update"}
						</button>
					</div>
				</form>
			</div>
		</div>
	);
}
