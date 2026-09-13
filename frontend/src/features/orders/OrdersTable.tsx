import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from "@tanstack/react-table";
import { useVirtualizer } from "@tanstack/react-virtual";
import { useMemo, useRef } from "react";
import { StatusBadge, THead, Table, Td, Th, Tr, orderTone, statusLabel } from "../../components/ui";
import type { Order } from "./useOrders";

interface OrdersTableProps {
	data: Order[];
	isLoading: boolean;
}

function SkeletonRow() {
	return (
		<Tr>
			{Array.from({ length: 5 }).map((_, i) => (
				// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton cells
				<Td key={i} className="px-5 py-4">
					<div
						className="h-3.5 animate-pulse rounded bg-card2-light dark:bg-card2-dark"
						style={{ width: `${40 + i * 12}%` }}
					/>
				</Td>
			))}
		</Tr>
	);
}

export function OrdersTable({ data, isLoading }: OrdersTableProps) {
	const columns = useMemo<ColumnDef<Order>[]>(
		() => [
			{
				accessorKey: "orderId",
				header: "Order ID",
				cell: (info) => (
					<span className="font-mono text-[12px] text-ink3-light dark:text-ink3-dark">
						{info.getValue<string>().slice(0, 8)}
					</span>
				),
			},
			{
				accessorKey: "productName",
				header: "Product",
				cell: (info) => (
					<span className="font-medium text-ink-light dark:text-ink-dark">{info.getValue<string>()}</span>
				),
			},
			{
				accessorKey: "status",
				header: "Status",
				cell: (info) => {
					const status = info.getValue<string>();
					return <StatusBadge tone={orderTone(status)}>{statusLabel[status] ?? status}</StatusBadge>;
				},
			},
			{
				accessorKey: "quantity",
				header: "Qty",
				cell: (info) => (
					<span className="tabular-nums text-ink2-light dark:text-ink2-dark">
						{info.getValue<number>().toLocaleString()}
					</span>
				),
			},
			{
				accessorKey: "createdAt",
				header: "Created",
				cell: (info) => (
					<span className="text-[13px] text-ink3-light dark:text-ink3-dark">
						{new Date(info.getValue<string>()).toLocaleString()}
					</span>
				),
			},
		],
		[],
	);

	const table = useReactTable({
		data,
		columns,
		getCoreRowModel: getCoreRowModel(),
	});

	const { rows } = table.getRowModel();
	const parentRef = useRef<HTMLDivElement>(null);

	const rowVirtualizer = useVirtualizer({
		count: rows.length,
		getScrollElement: () => parentRef.current,
		estimateSize: () => 52,
		overscan: 10,
	});

	const virtualRows = rowVirtualizer.getVirtualItems();
	const totalSize = rowVirtualizer.getTotalSize();

	const paddingTop = virtualRows.length > 0 ? virtualRows[0].start : 0;
	const paddingBottom = virtualRows.length > 0 ? totalSize - virtualRows[virtualRows.length - 1].end : 0;

	if (isLoading) {
		return (
			<div className="overflow-hidden rounded-xl border border-line-light bg-card-light shadow-card dark:border-line-dark dark:bg-card-dark">
				<Table>
					<THead>
						{columns.map((column) => (
							<Th key={String(column.header)}>{column.header as string}</Th>
						))}
					</THead>
					<tbody>
						{Array.from({ length: 8 }).map((_, i) => (
							// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton rows
							<SkeletonRow key={i} />
						))}
					</tbody>
				</Table>
			</div>
		);
	}

	return (
		<div
			ref={parentRef}
			className="h-[560px] overflow-auto rounded-xl border border-line-light bg-card-light shadow-card custom-scrollbar dark:border-line-dark dark:bg-card-dark"
		>
			<table className="w-full border-collapse text-left">
				<thead className="sticky top-0 z-10">
					<tr className="border-b border-line-light bg-card2-light/90 backdrop-blur dark:border-line-dark dark:bg-card2-dark/90">
						{table.getHeaderGroups().map((headerGroup) => (
							<th key={headerGroup.id} colSpan={headerGroup.headers.length} className="p-0">
								<div className="flex">
									{headerGroup.headers.map((header) => (
										<div
											key={header.id}
											className="px-5 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-ink3-light dark:text-ink3-dark"
										>
											{flexRender(header.column.columnDef.header, header.getContext())}
										</div>
									))}
								</div>
							</th>
						))}
					</tr>
				</thead>
				<tbody>
					{paddingTop > 0 && (
						<tr>
							<td colSpan={columns.length} style={{ height: `${paddingTop}px` }} />
						</tr>
					)}
					{virtualRows.map((virtualRow) => {
						const row = rows[virtualRow.index];
						return (
							<tr
								key={row.id}
								className="border-b border-line-light transition-colors last:border-0 hover:bg-card2-light/60 dark:border-line-dark dark:hover:bg-card2-dark/50"
							>
								{row.getVisibleCells().map((cell) => (
									<td key={cell.id} className="whitespace-nowrap px-5 py-3 text-sm text-ink2-light dark:text-ink2-dark">
										{flexRender(cell.column.columnDef.cell, cell.getContext())}
									</td>
								))}
							</tr>
						);
					})}
					{paddingBottom > 0 && (
						<tr>
							<td colSpan={columns.length} style={{ height: `${paddingBottom}px` }} />
						</tr>
					)}
				</tbody>
			</table>
		</div>
	);
}
