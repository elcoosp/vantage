import { Plus, Tag } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
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

	const updateMutation = useMutation({
		mutationFn: (data: { id: string; product: ProductRequest }) =>
			apiClient.put<Product>(`/products/${data.id}`, data.product),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["products"] });
			queryClient.invalidateQueries({ queryKey: ["dashboard-products"] });
			setEditingProduct(null);
			toast.success("Product updated successfully");
		},
		onError: () => {
			toast.error("Failed to update product");
		},
	});

	const handleEdit = (product: Product) => {
		setEditingProduct(product);
	};

	const handleUpdate = (e: React.FormEvent<HTMLFormElement>) => {
		e.preventDefault();
		if (!editingProduct) return;
		const form = e.currentTarget;
		const data: ProductRequest = {
			name: (form.elements.namedItem("name") as HTMLInputElement).value,
			description: (form.elements.namedItem("description") as HTMLInputElement).value,
			price: parseFloat((form.elements.namedItem("price") as HTMLInputElement).value),
		};
		updateMutation.mutate({ id: editingProduct.id, product: data });
	};

	const { data: products, isLoading, error } = useQuery({
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
			toast.success("Product created successfully");
		},
		onError: () => {
			toast.error("Failed to create product");
		},
	});

	const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
		e.preventDefault();
		const form = e.currentTarget;
		const data: ProductRequest = {
			name: (form.elements.namedItem("name") as HTMLInputElement).value,
			description: (form.elements.namedItem("description") as HTMLInputElement).value,
			price: parseFloat((form.elements.namedItem("price") as HTMLInputElement).value),
		};
		createMutation.mutate(data);
	};

	if (isLoading) return <div className="p-4">Loading products...</div>;
	if (error) return <div className="p-4 text-red-600">Failed to load products</div>;

	return (
		<div className="space-y-6">
			<div className="flex justify-between items-center">
				<h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">Products</h1>
				<button
					type="button"
					onClick={() => setShowCreate(true)}
					className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
				>
					<Plus className="h-4 w-4" />
					Add Product
				</button>
			</div>

			{showCreate && (
				<div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
					<div className="bg-white dark:bg-slate-800 rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
						<h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 mb-4">
							Add New Product
						</h2>
						<form onSubmit={handleSubmit} className="space-y-4">
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Name
								</label>
								<input
									name="name"
									type="text"
									required
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Description
								</label>
								<input
									name="description"
									type="text"
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Price
								</label>
								<input
									name="price"
									type="number"
									step="0.01"
									required
									min="0"
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div className="flex gap-3 pt-2">
								<button
									type="submit"
									disabled={createMutation.isPending}
									className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
								>
									{createMutation.isPending ? "Creating..." : "Create"}
								</button>
								<button
									type="button"
									onClick={() => setShowCreate(false)}
									className="flex-1 px-4 py-2 border border-slate-300 dark:border-slate-600 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700"
								>
									Cancel
								</button>
							</div>
						</form>
					</div>
				</div>
			)}

			{editingProduct && (
				<div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
					<div className="bg-white dark:bg-slate-800 rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
						<h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 mb-4">
							Edit Product
						</h2>
						<form onSubmit={handleUpdate} className="space-y-4">
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Name
								</label>
								<input
									name="name"
									type="text"
									required
									defaultValue={editingProduct.name}
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Description
								</label>
								<input
									name="description"
									type="text"
									defaultValue={editingProduct.description ?? ""}
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div>
								<label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
									Price
								</label>
								<input
									name="price"
									type="number"
									step="0.01"
									required
									min="0"
									defaultValue={editingProduct.price}
									className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
								/>
							</div>
							<div className="flex gap-3 pt-2">
								<button
									type="submit"
									disabled={updateMutation.isPending}
									className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
								>
									{updateMutation.isPending ? "Updating..." : "Update"}
								</button>
								<button
									type="button"
									onClick={() => setEditingProduct(null)}
									className="flex-1 px-4 py-2 border border-slate-300 dark:border-slate-600 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700"
								>
									Cancel
								</button>
							</div>
						</form>
					</div>
				</div>
			)}

			{products?.length === 0 ? (
				<div className="text-center py-12 text-slate-500 dark:text-slate-400">
					<div className="mb-2 w-fit mx-auto">
						<Tag className="h-8 w-8 text-slate-400" />
					</div>
					<p>No products yet. Click "Add Product" to get started.</p>
				</div>
			) : (
				<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
					{products?.map((product) => (
						<div
							key={product.id}
							className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-5"
						>
							<div className="flex justify-between items-start mb-3">
								<h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
									{product.name}
								</h3>
								<span className="text-xs text-slate-500 dark:text-slate-400 bg-slate-100 dark:bg-slate-700 px-2 py-1 rounded">
									{product.id.slice(0, 8)}
								</span>
							</div>
							{product.description && (
								<p className="text-sm text-slate-600 dark:text-slate-300 mb-3">
									{product.description}
								</p>
							)}
							<div className="text-xl font-bold text-blue-600 dark:text-blue-400 mb-3">
								${product.price.toFixed(2)}
							</div>
							<button
								type="button"
								onClick={() => handleEdit(product)}
								className="w-full px-3 py-2 text-sm bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-600"
							>
								Edit
							</button>
						</div>
					))}
				</div>
			)}
		</div>
	);
}
