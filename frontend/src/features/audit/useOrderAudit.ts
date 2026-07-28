export interface AuditEvent {
  id: string;
  eventType: string;
  payload: string;
  createdAt: string;
}

export function useOrderAudit(_orderId: string) {
  return { events: [], isLoading: false, error: null };
}
