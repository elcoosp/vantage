import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
	token: string | null;
	tenantId: string | null;
	setAuth: (token: string, tenantId: string) => void;
	clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
	persist(
		(set) => ({
			token: null,
			tenantId: null,
			setAuth: (token, tenantId) => set({ token, tenantId }),
			clearAuth: () => set({ token: null, tenantId: null }),
		}),
		{ name: "auth-storage" },
	),
);
