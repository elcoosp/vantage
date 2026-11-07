import { useQuery } from "@tanstack/react-query";
import apiClient from "../../lib/api";

export interface AuditEvent {
        id: string;
        eventType: string;
        payload: string;
        createdAt: string;
}

interface ApiAuditEvent {
        id: string;
        aggregateType: string;
        aggregateId: string;
        eventType: string;
        payload: string;
        createdAt: string;
}

/**
 * Loads the audit timeline for one order from the real endpoint
 * (GET /api/v1/audit/orders/{orderId}).
 */
export function useOrderAudit(orderId: string | null) {
        return useQuery({
                queryKey: ["orderAudit", orderId],
                queryFn: async () => {
                        const res = await apiClient.get<ApiAuditEvent[]>(`/audit/orders/${orderId}`);
                        return res.data.map(
                                (event): AuditEvent => ({
                                        id: event.id,
                                        eventType: event.eventType,
                                        payload: event.payload,
                                        createdAt: event.createdAt,
                                }),
                        );
                },
                enabled: Boolean(orderId),
                staleTime: 5000,
        });
}
