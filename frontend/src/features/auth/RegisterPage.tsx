import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthLayout } from "../../components/AuthLayout";
import { Button, Field, Input } from "../../components/ui";
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
			const response = await register({ email, password, name: storeName });
			setAuth(response.token, response.tenantId);
			navigate("/");
		} catch (err: unknown) {
			const errorObj = err as { response?: { data?: { message?: string } } };
			const message = errorObj.response?.data?.message || (err instanceof Error ? err.message : "Registration failed");
			setError(message);
		} finally {
			setLoading(false);
		}
	};

	return (
		<AuthLayout title="Create your account" subtitle="Set up your merchant workspace in under a minute.">
			<form className="space-y-5" onSubmit={handleSubmit}>
				<div className="space-y-4">
					<Field label="Store name">
						<Input
							id="storeName"
							type="text"
							required
							autoComplete="organization"
							autoFocus
							placeholder="Northwind Emporium"
							value={storeName}
							onChange={(e) => setStoreName(e.target.value)}
						/>
					</Field>
					<Field label="Work email">
						<Input
							id="email"
							type="email"
							required
							autoComplete="email"
							placeholder="you@store.com"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
						/>
					</Field>
					<Field label="Password">
						<Input
							id="password"
							type="password"
							required
							autoComplete="new-password"
							placeholder="Minimum 8 characters"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
						/>
					</Field>
					<Field label="Store slug">
						<Input
							id="storeSlug"
							type="text"
							required
							placeholder="northwind"
							value={storeSlug}
							onChange={(e) => setStoreSlug(e.target.value)}
						/>
					</Field>
				</div>

				{error && (
					<p className="rounded-lg bg-status-danger-soft-light px-3 py-2 text-sm text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]">
						{error}
					</p>
				)}

				<Button type="submit" disabled={loading} className="w-full" size="md">
					{loading ? "Creating account…" : "Create account"}
				</Button>

				<p className="text-center text-sm text-ink3-light dark:text-ink3-dark">
					Already registered?{" "}
					<a
						href="/login"
						className="font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
					>
						Sign in
					</a>
				</p>
			</form>
		</AuthLayout>
	);
}
