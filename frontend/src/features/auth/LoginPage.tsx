import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthLayout } from "../../components/AuthLayout";
import { Button, Field, Input } from "../../components/ui";
import { useAuthStore } from "../../store/authStore";
import { login } from "./api";

export function LoginPage() {
	const navigate = useNavigate();
	const setAuth = useAuthStore((state) => state.setAuth);
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(false);

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setError(null);
		setLoading(true);
		try {
			const response = await login({ email, password });
			setAuth(response.token, response.tenantId);
			navigate("/");
		} catch (err: unknown) {
			const errorObj = err as { response?: { data?: { message?: string } } };
			const message = errorObj.response?.data?.message || (err instanceof Error ? err.message : "Login failed");
			setError(message);
		} finally {
			setLoading(false);
		}
	};

	return (
		<AuthLayout title="Welcome back" subtitle="Sign in to your merchant workspace.">
			<form className="space-y-5" onSubmit={handleSubmit}>
				<div className="space-y-4">
					<Field label="Email">
						<Input
							id="email"
							type="email"
							required
							autoComplete="email"
							autoFocus
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
							autoComplete="current-password"
							placeholder="••••••••"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
						/>
					</Field>
				</div>

				{error && (
					<p className="rounded-lg bg-status-danger-soft-light px-3 py-2 text-sm text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]">
						{error}
					</p>
				)}

				<Button type="submit" disabled={loading} className="w-full" size="md">
					{loading ? "Signing in…" : "Sign in"}
				</Button>

				<p className="text-center text-sm text-ink3-light dark:text-ink3-dark">
					New to Vantage?{" "}
					<a
						href="/register"
						className="font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
					>
						Create an account
					</a>
				</p>
			</form>
		</AuthLayout>
	);
}
