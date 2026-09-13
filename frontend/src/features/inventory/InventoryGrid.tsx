import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Package } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Button, Card, EmptyState, PageHeader, THead, Table, Td, Th, Tr } from "../../components/ui";
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

	const {
		data: inventory,
		isLoading,
		error,
	} = useQuery({
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
		mutationFn: ({ productId, quantity, version }: { productId: string; quantity: number; version: number }) =>
			updateInventory(productId, quantity, version),
		onError: () => {
			toast.error("Conflict: another change landed first. Please refresh and retry.");
		},
		onSettled: () => {
			queryClient.invalidateQueries({ queryKey: ["inventory"] });
		},
	});

	const [editingId, setEditingId] = useState<string | null>(null);

	const lowStock = (inventory ?? []).filter((i) => i.quantity <= 10);
	const safeQty = (n: number) => (Number.isFinite(n) ? Math.max(0, Math.round(n)) : 0);

	if (isLoading) {
		return (
			<div className="space-y-4">
				<div className="h-8 w-48 animate-pulse rounded-lg bg-card-light dark:bg-card-dark" />
				<Card>
					<div className="space-y-2 p-5">
						{Array.from({ length: 6 }).map((_, i) => (
							// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton rows
							<div key={i} className="h-8 animate-pulse rounded-md bg-card2-light dark:bg-card2-dark" />
						))}
					</div>
				</Card>
			</div>
		);
	}

	if (error) {
		return (
			<div className="rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
				Failed to load inventory.
			</div>
		);
	}

	if (!inventory || inventory.length === 0) {
		return (
			<div className="space-y-6 animate-fade-up">
				<PageHeader title="Inventory" description="Stock levels and reservations, with optimistic concurrency." />
				<Card>
					<EmptyState
						icon={<Package className="size-5" />}
						title="No inventory yet"
						description="Create products and they will appear here with trackable stock levels."
					/>
				</Card>
			</div>
		);
	}

	return (
		<div className="space-y-5 animate-fade-up">
			<PageHeader
				title="Inventory"
				description="Stock levels per product. Changes carry a version check, so concurrent edits surface as conflicts."
			/>

			{lowStock.length > 0 && (
				<div className="flex items-start gap-3 rounded-xl border border-status-warning-soft-light bg-status-warning-soft-light p-4 dark:border-[#D3932B]/25 dark:bg-[#D3932B]/10">
					<span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-lg bg-status-warning-solid-light/15 text-status-warning-ink-light dark:bg-[#D3932B]/20 dark:text-[#F0C468]">
						<AlertTriangle className="size-4" />
					</span>
					<p className="text-sm text-status-warning-ink-light dark:text-[#F0C468]">
						<strong>{lowStock.length}</strong> item{p(lowStock.length)} at or below the reorder point (≤ 10 units).
					</p>
				</div>
			)}

			<Card>
				<Table>
					<THead>
						<Th>Product</Th>
						<Th>ID</Th>
						<Th className="text-right">On hand</Th>
						<Th className="text-right">Version</Th>
						<Th className="text-right">Action</Th>
					</THead>
					<tbody>
						{inventory.map((item) => {
							const product = products?.find((p) => p.id === item.productId);
							const name = product?.name ?? `${item.productId.slice(0, 8)}…`;
							const isLow = item.quantity <= 10;
							const isEditing = editingId === item.productId;
							return (
								<Tr key={item.productId}>
									<Td className="font-medium text-ink-light dark:text-ink-dark">{name}</Td>
									<Td className="font-mono text-[12px] text-ink3-light dark:text-ink3-dark">
										{item.productId.slice(0, 8)}
									</Td>
									<Td className="text-right">
										<span
											className={
												isLow
													? "inline-flex items-center justify-end rounded-md bg-status-warning-soft-light px-2 py-0.5 text-[13px] font-semibold tabular-nums text-status-warning-ink-light dark:bg-[#D3932B]/15 dark:text-[#F0C468]"
													: "tabular-nums font-medium text-ink-light dark:text-ink-dark"
											}
										>
											{item.quantity}
										</span>
									</Td>
									<Td className="text-right font-mono text-[12px] text-ink3-light dark:text-ink3-dark">
										v{item.version}
									</Td>
									<Td className="text-right">
										{isEditing ? (
											<InventoryEditForm
												currentQuantity={item.quantity}
												onSubmit={(qty) => {
													mutation.mutate({
														productId: item.productId,
														quantity: safeQty(qty),
														version: item.version,
													});
													setEditingId(null);
												}}
												onCancel={() => setEditingId(null)}
												isPending={mutation.isPending}
											/>
										) : (
											<Button variant="secondary" size="sm" onClick={() => setEditingId(item.productId)}>
												Edit quantity
											</Button>
										)}
									</Td>
								</Tr>
							);
						})}
					</tbody>
				</Table>
			</Card>
		</div>
	);
}

function p(n: number) {
	return n === 1 ? "" : "s";
}
