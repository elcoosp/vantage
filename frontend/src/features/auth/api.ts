import api from "../../lib/api";

interface RegisterRequest {
	email: string;
	password: string;
	name: string;
	slug: string;
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

// Mock login – simulates successful login for any credentials.
// In a real implementation, this would call a backend /vendors/login endpoint.
export async function login(_data: LoginRequest): Promise<AuthResponse> {
	// Simulate network delay
	await new Promise((resolve) => setTimeout(resolve, 500));
	// Return a fake token and tenantId (matching what registration would produce)
	const fakeTenantId = "00000000-0000-0000-0000-000000000001";
	const fakeToken = "mock-jwt-token";
	return { token: fakeToken, tenantId: fakeTenantId };
}
