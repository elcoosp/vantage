import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../lib/api";
import { useUIStore } from "../store/uiStore";
import { Button, Field, Input, Modal, Select } from "./ui";

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

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		mutation.mutate();
	};

	return (
		<Modal
			open={updateStockModalOpen}
			onClose={closeUpdateStockModal}
			title="Update stock"
			description="Adjust the on-hand quantity for a product."
		>
			<form onSubmit={handleSubmit} className="space-y-4">
				<Field label="Product">
					<Select
						id="update-product"
						value={selectedProduct}
						onChange={(e) => {
							setSelectedProduct(e.target.value);
							const item = data?.find((i) => i.productId === e.target.value);
							if (item) setNewQuantity(item.quantity);
						}}
						required
						disabled={isLoading}
					>
						<option value="">Select a product</option>
						{data?.map((item) => (
							<option key={item.productId} value={item.productId}>
								{item.productId} (current: {item.quantity})
							</option>
						))}
					</Select>
				</Field>
				<Field label="New quantity">
					<Input
						id="update-quantity"
						type="number"
						min={0}
						value={newQuantity}
						onChange={(e) => setNewQuantity(Number(e.target.value))}
						required
					/>
				</Field>
				<div className="flex justify-end gap-2 pt-1">
					<Button type="button" variant="secondary" onClick={closeUpdateStockModal} disabled={mutation.isPending}>
						Cancel
					</Button>
					<Button type="submit" disabled={mutation.isPending || !selectedProduct}>
						{mutation.isPending ? "Updating…" : "Update stock"}
					</Button>
				</div>
			</form>
		</Modal>
	);
}
