import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthLayout } from "../../components/AuthLayout";
import { useAuthStore } from "../../store/authStore";
import { register } from "./api";

export function RegisterPage() {
	const navigate = useNavigate();
	const setAuth = useAuthStore((state) => state.setAuth);
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [storeName, setStoreName] = useState("");
	const [storeSlug, setStoreSlug] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(false);

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setError(null);
		setLoading(true);
		try {
			const response = await register({ email, password, name: storeName, slug: storeSlug });
			setAuth(response.token, response.tenantId);
			navigate("/dashboard");
		} catch (err: unknown) {
			const errorObj = err as { response?: { data?: { message?: string } } };
			const message = errorObj.response?.data?.message || (err instanceof Error ? err.message : "Registration failed");
			setError(message);
		} finally {
			setLoading(false);
		}
	};

	return (
		<AuthLayout title="Register your store">
			<form className="mt-8 space-y-6" onSubmit={handleSubmit}>
				<div className="rounded-md shadow-sm -space-y-px">
					<div>
						<label htmlFor="email" className="sr-only">
							Email
						</label>
						<input
							id="email"
							type="email"
							required
							className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
							placeholder="Email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
						/>
					</div>
					<div>
						<label htmlFor="password" className="sr-only">
							Password
						</label>
						<input
							id="password"
							type="password"
							required
							className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
							placeholder="Password (min 8 chars)"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
						/>
					</div>
					<div>
						<label htmlFor="storeName" className="sr-only">
							Store Name
						</label>
						<input
							id="storeName"
							type="text"
							required
							className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
							placeholder="Store Name"
							value={storeName}
							onChange={(e) => setStoreName(e.target.value)}
						/>
					</div>
					<div>
						<label htmlFor="storeSlug" className="sr-only">
							Store Slug
						</label>
						<input
							id="storeSlug"
							type="text"
							required
							className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
							placeholder="Store Slug (e.g., my-store)"
							value={storeSlug}
							onChange={(e) => setStoreSlug(e.target.value)}
						/>
					</div>
				</div>
				{error && <div className="text-sm text-red-600">{error}</div>}
				<div>
					<button
						type="submit"
						disabled={loading}
						className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
					>
						{loading ? "Registering..." : "Register"}
					</button>
				</div>
				<div className="text-sm text-center">
					Already have an account?{" "}
					<a href="/login" className="font-medium text-blue-600 hover:text-blue-500">
						Sign in
					</a>
				</div>
			</form>
		</AuthLayout>
	);
}
