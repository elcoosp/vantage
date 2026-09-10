import api from "../../lib/api";

interface RegisterRequest {
	email: string;
	password: string;
	name: string;
}

interface LoginRequest {
	email: string;
	password: string;
}

interface AuthResponse {
	token: string;
	tenantId: string;
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
	const response = await api.post<AuthResponse>("/vendors/register", data);
	return response.data;
}

export async function login(data: LoginRequest): Promise<AuthResponse> {
	const response = await api.post<AuthResponse>("/vendors/login", data);
	return response.data;
}
