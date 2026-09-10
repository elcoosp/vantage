import axios from "axios";
import { useAuthStore } from "../store/authStore";

const api = axios.create({
	baseURL: "/api/v1",
	headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
	const state = useAuthStore.getState();
	const token = state.accessToken ?? localStorage.getItem("accessToken");
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

api.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			useAuthStore.getState().clearAuth();
			localStorage.removeItem("accessToken");
			localStorage.removeItem("tenantId");
			window.location.href = "/login";
		}
		return Promise.reject(error);
	},
);

export default api;
