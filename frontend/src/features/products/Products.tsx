import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Tag } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Button, Card, EmptyState, Field, Input, Modal, PageHeader } from "../../components/ui";
import apiClient from "../../lib/api";

interface Product {
	id: string;
	name: string;
	description: string | null;
	price: number;
}

interface ProductRequest {
	name: string;
	description: string;
	price: number;
}

async function fetchProducts(): Promise<Product[]> {
	const res = await apiClient.get<Product[]>("/products");
	return res.data;
}

async function createProduct(data: ProductRequest): Promise<Product> {
	const res = await apiClient.post<Product>("/products", data);
	return res.data;
}

export function Products() {
	const queryClient = useQueryClient();
	const [showCreate, setShowCreate] = useState(false);
	const [editingProduct, setEditingProduct] = useState<Product | null>(null);
	const [createError, setCreateError] = useState<string | null>(null);
	const [updateError, setUpdateError] = useState<string | null>(null);

	const updateMutation = useMutation({
		mutationFn: (data: { id: string; product: ProductRequest }) =>
			apiClient.put<Product>(`/products/${data.id}`, data.product),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["products"] });
			queryClient.invalidateQueries({ queryKey: ["dashboard-products"] });
			setEditingProduct(null);
			toast.success("Product updated");
		},
		onError: () => {
			setUpdateError("Could not update the product. Please try again.");
		},
	});

	const handleEdit = (product: Product) => {
		setEditingProduct(product);
		setUpdateError(null);
	};

	const handleUpdate = (e: React.FormEvent<HTMLFormElement>) => {
		e.preventDefault();
		if (!editingProduct) return;
		const form = e.currentTarget;
		const data: ProductRequest = {
			name: (form.elements.namedItem("name") as HTMLInputElement).value,
			description: (form.elements.namedItem("description") as HTMLInputElement).value,
			price: Number.parseFloat((form.elements.namedItem("price") as HTMLInputElement).value),
		};
		updateMutation.mutate({ id: editingProduct.id, product: data });
	};

	const {
		data: products,
		isLoading,
		error,
	} = useQuery({
		queryKey: ["products"],
		queryFn: fetchProducts,
		retry: false,
	});

	const createMutation = useMutation({
		mutationFn: createProduct,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["products"] });
			queryClient.invalidateQueries({ queryKey: ["dashboard-products"] });
			setShowCreate(false);
			toast.success("Product created");
		},
		onError: () => {
			setCreateError("Could not create the product. Please try again.");
		},
	});

	const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
		e.preventDefault();
		const form = e.currentTarget;
		const data: ProductRequest = {
			name: (form.elements.namedItem("name") as HTMLInputElement).value,
			description: (form.elements.namedItem("description") as HTMLInputElement).value,
			price: Number.parseFloat((form.elements.namedItem("price") as HTMLInputElement).value),
		};
		createMutation.mutate(data);
	};

	if (isLoading) {
		return (
			<div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
				{Array.from({ length: 6 }).map((_, i) => (
					// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton cards
					<div key={i} className="h-40 animate-pulse rounded-xl bg-card-light dark:bg-card-dark" />
				))}
			</div>
		);
	}

	if (error) {
		return (
			<div className="rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
				Failed to load products.
			</div>
		);
	}

	return (
		<div className="space-y-6 animate-fade-up">
			<PageHeader
				title="Products"
				description="Your product catalog — pricing, descriptions, and identifiers."
				actions={
					<Button onClick={() => setShowCreate(true)}>
						<Plus className="size-4" />
						Add product
					</Button>
				}
			/>

			{products?.length === 0 ? (
				<Card>
					<EmptyState
						icon={<Tag className="size-5" />}
						title="No products yet"
						description="Add your first product and it will appear in the catalog, ready to sell."
						action={
							<Button onClick={() => setShowCreate(true)}>
								<Plus className="size-4" />
								Add product
							</Button>
						}
					/>
				</Card>
			) : (
				<div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
					{products?.map((product) => (
						<Card key={product.id} hover className="flex flex-col p-5">
							<div className="flex items-start justify-between gap-3">
								<h3 className="truncate text-[15px] font-semibold tracking-tight text-ink-light dark:text-ink-dark">
									{product.name}
								</h3>
								<span className="shrink-0 rounded-md bg-card2-light px-1.5 py-0.5 font-mono text-[11px] text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
									{product.id.slice(0, 8)}
								</span>
							</div>
							{product.description && (
								<p className="mt-2 line-clamp-2 text-[13px] leading-relaxed text-ink3-light dark:text-ink3-dark">
									{product.description}
								</p>
							)}
							<div className="mt-4 flex items-end justify-between gap-3 border-t border-line-light pt-4 dark:border-line-dark">
								<span className="text-xl font-semibold tabular-nums tracking-tight text-ink-light dark:text-ink-dark">
									{new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(product.price)}
								</span>
								<Button variant="secondary" size="sm" onClick={() => handleEdit(product)}>
									Edit
								</Button>
							</div>
						</Card>
					))}
				</div>
			)}

			{/* Create modal */}
			<Modal
				open={showCreate}
				onClose={() => setShowCreate(false)}
				title="Add product"
				description="A new item in your catalog."
			>
				<form onSubmit={handleSubmit} className="space-y-4">
					<Field label="Name">
						<Input name="name" type="text" required placeholder="e.g. Organic coffee beans" />
					</Field>
					<Field label="Description">
						<Input name="description" type="text" placeholder="Short, customer-facing summary" />
					</Field>
					<Field label="Price">
						<Input name="price" type="number" step="0.01" required min="0" placeholder="0.00" />
					</Field>
					{createError && (
						<p className="rounded-lg bg-status-danger-soft-light px-3 py-2 text-sm text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]">
							{createError}
						</p>
					)}
					<div className="flex gap-3 pt-1">
						<Button type="submit" disabled={createMutation.isPending} className="flex-1">
							{createMutation.isPending ? "Creating…" : "Create product"}
						</Button>
						<Button type="button" variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">
							Cancel
						</Button>
					</div>
				</form>
			</Modal>

			{/* Edit modal */}
			<Modal
				open={editingProduct != null}
				onClose={() => setEditingProduct(null)}
				title="Edit product"
				description="Update details for this item."
			>
				<form onSubmit={handleUpdate} className="space-y-4">
					<Field label="Name">
						<Input name="name" type="text" required defaultValue={editingProduct?.name} />
					</Field>
					<Field label="Description">
						<Input name="description" type="text" defaultValue={editingProduct?.description ?? ""} />
					</Field>
					<Field label="Price">
						<Input name="price" type="number" step="0.01" required min="0" defaultValue={editingProduct?.price} />
					</Field>
					{updateError && (
						<p className="rounded-lg bg-status-danger-soft-light px-3 py-2 text-sm text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]">
							{updateError}
						</p>
					)}
					<div className="flex gap-3 pt-1">
						<Button type="submit" disabled={updateMutation.isPending} className="flex-1">
							{updateMutation.isPending ? "Saving…" : "Save changes"}
						</Button>
						<Button type="button" variant="secondary" onClick={() => setEditingProduct(null)} className="flex-1">
							Cancel
						</Button>
					</div>
				</form>
			</Modal>
		</div>
	);
}
