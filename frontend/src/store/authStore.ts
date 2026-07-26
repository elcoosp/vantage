import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
	accessToken: string | null;
	tenantId: string | null;
	setAuth: (token: string, tenantId: string) => void;
	clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
	persist(
		(set) => ({
			accessToken: null,
			tenantId: null,
			setAuth: (token, tenantId) => set({ accessToken: token, tenantId }),
			clearAuth: () => set({ accessToken: null, tenantId: null }),
		}),
		{
			name: "auth-storage",
			partialize: (state) => ({ accessToken: state.accessToken, tenantId: state.tenantId }),
		},
	),
);
