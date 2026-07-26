import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../lib/api";
import { useUIStore } from "../store/uiStore";

export function AddProductModal() {
	const { addProductModalOpen, closeAddProductModal } = useUIStore();
	const [name, setName] = useState("");
	const [price, setPrice] = useState("");
	const [description, setDescription] = useState("");
	const queryClient = useQueryClient();

	const mutation = useMutation({
		mutationFn: async () => {
			await apiClient.post("/products", { name, price: Number.parseFloat(price), description });
		},
		onSuccess: () => {
			toast.success("Product added successfully");
			queryClient.invalidateQueries({ queryKey: ["inventory"] });
			closeAddProductModal();
			setName("");
			setPrice("");
			setDescription("");
		},
		onError: () => {
			toast.error("Failed to add product");
		},
	});

	if (!addProductModalOpen) return null;

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		mutation.mutate();
	};

	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
			<div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
				<h2 className="text-xl font-bold mb-4">Add New Product</h2>
				<form onSubmit={handleSubmit}>
					<div className="mb-3">
						<label htmlFor="product-name" className="block text-sm font-medium mb-1">
							Name
						</label>
						<input
							type="text"
							value={name}
							onChange={(e) => setName(e.target.value)}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							required
						/>
					</div>
					<div className="mb-3">
						<label htmlFor="product-price" className="block text-sm font-medium mb-1">
							Price
						</label>
						<input
							type="number"
							step="0.01"
							value={price}
							onChange={(e) => setPrice(e.target.value)}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							required
						/>
					</div>
					<div className="mb-3">
						<label htmlFor="product-description" className="block text-sm font-medium mb-1">
							Description
						</label>
						<textarea
							value={description}
							onChange={(e) => setDescription(e.target.value)}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							rows={3}
						/>
					</div>
					<div className="flex justify-end gap-2">
						<button
							type="button"
							onClick={closeAddProductModal}
							className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
							disabled={mutation.isPending}
						>
							Cancel
						</button>
						<button
							type="submit"
							className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
							disabled={mutation.isPending}
						>
							{mutation.isPending ? "Adding..." : "Add Product"}
						</button>
					</div>
				</form>
			</div>
		</div>
	);
}
