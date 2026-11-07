import { useMutation, useQueryClient } from "@tanstack/react-query";
import { History, PackageCheck, Search, Truck, X } from "lucide-react";
import { useState } from "react";
import {
        Button,
        Card,
        EmptyState,
        Input,
        PageHeader,
        StatusBadge,
        orderTone,
        statusLabel,
} from "../../components/ui";
import { AuditTimeline } from "../audit/AuditTimeline";
import { useOrderAudit } from "../audit/useOrderAudit";
import apiClient from "../../lib/api";
import { OrdersTable } from "./OrdersTable";
import { useOrders, type Order } from "./useOrders";

export function OrdersPage() {
        const { data, isLoading, isError } = useOrders();
        const [searchTerm, setSearchTerm] = useState("");
        const [selected, setSelected] = useState<Order | null>(null);
        const queryClient = useQueryClient();

        const filteredContent =
                data?.content.filter(
                        (order) =>
                                order.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                                order.status.toLowerCase().includes(searchTerm.toLowerCase()) ||
                                order.orderId.toLowerCase().includes(searchTerm.toLowerCase()),
                ) ?? [];

        const advanceMutation = useMutation({
                mutationFn: async ({ orderId, action }: { orderId: string; action: "ship" | "deliver" }) => {
                        const res = await apiClient.patch(`/orders/${orderId}/${action}`);
                        return res.data;
                },
                onSuccess: (_result, variables) => {
                        queryClient.invalidateQueries({ queryKey: ["orders"] });
                        queryClient.invalidateQueries({ queryKey: ["orderAudit", variables.orderId] });
                },
        });

        // selected is a snapshot; the live status comes from the query cache so
        // the panel badge updates after ship/deliver/cancel round-trips.
        const liveSelected = selected
                ? (data?.content.find((order) => order.orderId === selected.orderId) ?? selected)
                : null;
        const selectedIsPaid = liveSelected?.status === "PAID";
        const selectedIsShipped = liveSelected?.status === "SHIPPED";

        return (
                <div className="space-y-5 animate-fade-up">
                        <PageHeader
                                title="Orders"
                                description="Search and inspect every order across your storefront."
                                actions={
                                        data && (
                                                <span className="rounded-md bg-card2-light px-2.5 py-1 font-mono text-[12px] text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
                                                        {filteredContent.length.toLocaleString()} / {data.totalElements.toLocaleString()}
                                                </span>
                                        )
                                }
                        />

                        <div className="relative max-w-md">
                                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink3-light dark:text-ink3-dark" />
                                <Input
                                        type="text"
                                        id="order-search"
                                        placeholder="Search by product, status, or order ID…"
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        className="pl-9"
                                />
                        </div>

                        {isError && (
                                <div className="rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
                                        Failed to load orders. Please try again later.
                                </div>
                        )}

                        {!isLoading && (!data?.content || data.content.length === 0) ? (
                                <div className="rounded-xl border border-line-light bg-card-light p-4 dark:border-line-dark dark:bg-card-dark">
                                        <EmptyState
                                                icon={<Search className="size-5" />}
                                                title="No orders found"
                                                description="Orders will appear here once your storefront starts receiving requests."
                                        />
                                </div>
                        ) : (
                                <OrdersTable data={filteredContent} isLoading={isLoading} onRowClick={setSelected} />
                        )}

                        {liveSelected && (
                                <OrderDetailPanel
                                        order={liveSelected}
                                        onClose={() => setSelected(null)}
                                        onAdvance={(action) =>
                                                advanceMutation.mutate({ orderId: liveSelected.orderId, action })
                                        }
                                        isAdvancing={advanceMutation.isPending}
                                        advanceError={advanceMutation.error?.message ?? null}
                                        canShip={selectedIsPaid}
                                        canDeliver={selectedIsShipped}
                                />
                        )}
                </div>
        );
}

function OrderDetailPanel({
        order,
        onClose,
        onAdvance,
        isAdvancing,
        advanceError,
        canShip,
        canDeliver,
}: {
        order: Order;
        onClose: () => void;
        onAdvance: (action: "ship" | "deliver") => void;
        isAdvancing: boolean;
        advanceError: string | null;
        canShip: boolean;
        canDeliver: boolean;
}) {
        const { data: events, isLoading } = useOrderAudit(order.orderId);

        return (
                <Card className="animate-fade-up" data-testid="order-detail-panel">
                        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-line-light pb-4 dark:border-line-dark">
                                <div className="min-w-0">
                                        <div className="flex flex-wrap items-center gap-2.5">
                                                <h3 className="font-mono text-[13px] font-semibold text-ink-light dark:text-ink-dark">
                                                        {order.orderId.slice(0, 8)}
                                                </h3>
                                                <StatusBadge tone={orderTone(order.status)}>
                                                        {statusLabel[order.status] ?? order.status}
                                                </StatusBadge>
                                        </div>
                                        <p className="mt-1 truncate text-[13px] text-ink2-light dark:text-ink2-dark">
                                                {order.productName} · qty {order.quantity.toLocaleString()} · created{" "}
                                                {new Date(order.createdAt).toLocaleString()}
                                        </p>
                                </div>
                                <div className="flex items-center gap-2">
                                        <Button
                                                variant="secondary"
                                                size="sm"
                                                disabled={!canShip || isAdvancing}
                                                onClick={() => onAdvance("ship")}
                                                title={canShip ? "Mark as shipped" : "Only paid orders can be shipped"}
                                        >
                                                <Truck className="size-3.5" />
                                                Mark shipped
                                        </Button>
                                        <Button
                                                variant="secondary"
                                                size="sm"
                                                disabled={!canDeliver || isAdvancing}
                                                onClick={() => onAdvance("deliver")}
                                                title={canDeliver ? "Mark as delivered" : "Only shipped orders can be delivered"}
                                        >
                                                <PackageCheck className="size-3.5" />
                                                Mark delivered
                                        </Button>
                                        <button
                                                type="button"
                                                onClick={onClose}
                                                aria-label="Close order details"
                                                className="rounded-md p-1.5 text-ink3-light transition-colors hover:bg-card2-light hover:text-ink-light dark:text-ink3-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark"
                                        >
                                                <X className="size-4" />
                                        </button>
                                </div>
                        </div>

                        {advanceError && (
                                <p className="mt-3 rounded-lg bg-status-danger-soft-light px-3 py-2 text-[13px] text-status-danger-ink-light dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
                                        {advanceError}
                                </p>
                        )}

                        <div className="pt-4">
                                <div className="mb-3 flex items-center gap-2 text-[13px] font-semibold text-ink-light dark:text-ink-dark">
                                        <History className="size-4" />
                                        Audit timeline
                                </div>
                                {isLoading ? (
                                        <p className="text-[13px] text-ink3-light dark:text-ink3-dark">Loading audit events…</p>
                                ) : (
                                        <AuditTimeline events={events ?? []} />
                                )}
                        </div>
                </Card>
        );
}
