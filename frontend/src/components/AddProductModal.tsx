import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import apiClient from "../lib/api";
import { useUIStore } from "../store/uiStore";
import { Button, Field, Input, Modal } from "./ui";

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

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		mutation.mutate();
	};

	return (
		<Modal
			open={addProductModalOpen}
			onClose={closeAddProductModal}
			title="Add product"
			description="Create a new item in your catalog."
		>
			<form onSubmit={handleSubmit} className="space-y-4">
				<Field label="Name">
					<Input
						id="product-name"
						type="text"
						value={name}
						onChange={(e) => setName(e.target.value)}
						placeholder="e.g. Organic coffee beans"
						required
					/>
				</Field>
				<Field label="Price">
					<Input
						id="product-price"
						type="number"
						step="0.01"
						value={price}
						onChange={(e) => setPrice(e.target.value)}
						placeholder="0.00"
						required
					/>
				</Field>
				<Field label="Description">
					<textarea
						id="product-description"
						value={description}
						onChange={(e) => setDescription(e.target.value)}
						rows={3}
						placeholder="Short, customer-facing summary"
						className="w-full rounded-lg border border-line-light bg-card2-light px-3 py-2 text-sm text-ink-light outline-none transition-shadow placeholder:text-ink3-light focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 dark:border-line-dark dark:bg-card2-dark dark:text-ink-dark dark:placeholder:text-ink3-dark"
					/>
				</Field>
				<div className="flex justify-end gap-2 pt-1">
					<Button type="button" variant="secondary" onClick={closeAddProductModal} disabled={mutation.isPending}>
						Cancel
					</Button>
					<Button type="submit" disabled={mutation.isPending}>
						{mutation.isPending ? "Adding…" : "Add product"}
					</Button>
				</div>
			</form>
		</Modal>
	);
}
