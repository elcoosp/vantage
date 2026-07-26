import { type ColumnDef, flexRender, getCoreRowModel, useReactTable } from "@tanstack/react-table";
import { useVirtualizer } from "@tanstack/react-virtual";
import { useMemo, useRef } from "react";
import type { Order } from "./useOrders";

interface OrdersTableProps {
	data: Order[];
	isLoading: boolean;
}

function SkeletonRow() {
	return (
		<tr className="border-b border-slate-800">
			{Array.from({ length: 5 }).map((_, i) => (
				// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton cells
				<td key={i} className="px-6 py-4">
					<div className="h-4 bg-slate-700 rounded animate-pulse w-full" />
				</td>
			))}
		</tr>
	);
}

export function OrdersTable({ data, isLoading }: OrdersTableProps) {
	const columns = useMemo<ColumnDef<Order>[]>(
		() => [
			{
				accessorKey: "orderId",
				header: "Order ID",
				cell: (info) => (
					<span className="font-mono text-xs text-slate-300">{info.getValue<string>().slice(0, 8)}...</span>
				),
			},
			{
				accessorKey: "productName",
				header: "Product Name",
				cell: (info) => <span className="text-slate-100 font-medium">{info.getValue<string>()}</span>,
			},
			{
				accessorKey: "status",
				header: "Status",
				cell: (info) => {
					const status = info.getValue<string>();
					const colorMap: Record<string, string> = {
						CREATED: "bg-blue-900/50 text-blue-200 border-blue-700",
						CONFIRMED: "bg-purple-900/50 text-purple-200 border-purple-700",
						PAID: "bg-green-900/50 text-green-200 border-green-700",
						CANCELLED: "bg-red-900/50 text-red-200 border-red-700",
					};
					const color = colorMap[status] ?? "bg-slate-700 text-slate-300 border-slate-600";
					return <span className={`px-2 py-1 text-xs font-semibold rounded border ${color}`}>{status}</span>;
				},
			},
			{
				accessorKey: "quantity",
				header: "Quantity",
				cell: (info) => <span className="text-slate-200 tabular-nums">{info.getValue<number>().toLocaleString()}</span>,
			},
			{
				accessorKey: "createdAt",
				header: "Created At",
				cell: (info) => (
					<span className="text-slate-400 text-sm">{new Date(info.getValue<string>()).toLocaleString()}</span>
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
		estimateSize: () => 56,
		overscan: 10,
	});

	const virtualRows = rowVirtualizer.getVirtualItems();
	const totalSize = rowVirtualizer.getTotalSize();

	const paddingTop = virtualRows.length > 0 ? virtualRows[0].start : 0;
	const paddingBottom = virtualRows.length > 0 ? totalSize - virtualRows[virtualRows.length - 1].end : 0;

	if (isLoading) {
		return (
			<div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden">
				<table className="w-full">
					<thead className="bg-slate-900/50 sticky top-0 z-10">
						{table.getHeaderGroups().map((headerGroup) => (
							<tr key={headerGroup.id}>
								{headerGroup.headers.map((header) => (
									<th
										key={header.id}
										className="px-6 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-700"
									>
										{flexRender(header.column.columnDef.header, header.getContext())}
									</th>
								))}
							</tr>
						))}
					</thead>
					<tbody>
						{Array.from({ length: 10 }).map((_, i) => (
							// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton rows
							<SkeletonRow key={i} />
						))}
					</tbody>
				</table>
			</div>
		);
	}

	return (
		<div
			ref={parentRef}
			className="bg-slate-800 rounded-lg border border-slate-700 overflow-auto h-[600px] custom-scrollbar"
		>
			<table className="w-full">
				<thead className="bg-slate-900/80 backdrop-blur sticky top-0 z-10">
					{table.getHeaderGroups().map((headerGroup) => (
						<tr key={headerGroup.id}>
							{headerGroup.headers.map((header) => (
								<th
									key={header.id}
									className="px-6 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-700"
								>
									{flexRender(header.column.columnDef.header, header.getContext())}
								</th>
							))}
						</tr>
					))}
				</thead>
				<tbody>
					{paddingTop > 0 && (
						<tr>
							<td colSpan={columns.length} style={{ height: `${paddingTop}px` }} />
						</tr>
					)}
					{virtualRows.map((virtualRow) => {
						const row = rows[virtualRow.index];
						const isEven = virtualRow.index % 2 === 0;
						return (
							<tr
								key={row.id}
								className={`${
									isEven ? "bg-slate-800" : "bg-slate-800/50"
								} hover:bg-slate-700/50 transition-colors border-b border-slate-700/50`}
							>
								{row.getVisibleCells().map((cell) => (
									<td key={cell.id} className="px-6 py-4 whitespace-nowrap">
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
