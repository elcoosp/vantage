import { useAuthStore } from "../store/authStore";

/**
 * Resolves the tenant JWT for non-axios clients (SSE, STOMP, fetch streams).
 *
 * The zustand store persists to localStorage under "auth-storage"; older
 * builds wrote a raw "accessToken" key, so both are accepted. Nothing in the
 * app should read "token" — that key is never written and was the reason the
 * chat widget and ops map used to 401.
 */
export function getToken(): string | null {
        const fromStore = useAuthStore.getState().accessToken;
        if (fromStore) return fromStore;
        try {
                const raw = localStorage.getItem("accessToken");
                if (raw) return raw;
                const persisted = localStorage.getItem("auth-storage");
                if (persisted) {
                        const parsed = JSON.parse(persisted) as { state?: { accessToken?: string | null } };
                        return parsed?.state?.accessToken ?? null;
                }
        } catch {
                // corrupt storage — treat as signed out
        }
        return null;
}

export function getTenantId(): string | null {
        const fromStore = useAuthStore.getState().tenantId;
        if (fromStore) return fromStore;
        try {
                const persisted = localStorage.getItem("auth-storage");
                if (persisted) {
                        const parsed = JSON.parse(persisted) as { state?: { tenantId?: string | null } };
                        return parsed?.state?.tenantId ?? null;
                }
        } catch {
                // corrupt storage — treat as signed out
        }
        return null;
}
