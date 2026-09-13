import { Webhook } from "lucide-react";
import { useCallback, useState } from "react";
import toast from "react-hot-toast";
import { Button, Card, CardHeader, Field, Input } from "../../components/ui";
import { useWebhooks } from "./useWebhooks";

export function WebhooksPanel() {
	const { config, isLoading, updateWebhook, isUpdating, regenerateSecret, isRegenerating } = useWebhooks();
	const [webhookUrl, setWebhookUrl] = useState(config.webhookUrl);
	const [showSecret, setShowSecret] = useState<string | null>(null);

	const handleUpdate = useCallback(
		async (e: React.FormEvent) => {
			e.preventDefault();
			try {
				const result = await updateWebhook(webhookUrl);
				setShowSecret(result.secret);
				toast.success("Webhook updated");
			} catch (err) {
				toast.error("Failed to update webhook");
			}
		},
		[webhookUrl, updateWebhook],
	);

	const handleRegenerateSecret = useCallback(async () => {
		try {
			const result = await regenerateSecret();
			setShowSecret(result.secret);
			toast.success("New secret generated");
		} catch (err) {
			toast.error("Failed to regenerate secret");
		}
	}, [regenerateSecret]);

	const copyToClipboard = useCallback((text: string) => {
		navigator.clipboard
			.writeText(text)
			.then(() => toast.success("Copied to clipboard"))
			.catch(() => toast.error("Failed to copy"));
	}, []);

	return (
		<div className="max-w-xl">
			{isLoading && (
				<div className="text-[13px] text-ink3-light dark:text-ink3-dark">Loading webhook configuration…</div>
			)}

			{!isLoading && (
				<Card>
					<CardHeader
						title="Webhook endpoint"
						subtitle="We'll POST order events to this URL. Sign requests with the secret to verify them."
					/>
					<form onSubmit={handleUpdate} className="space-y-4 px-5 pb-5">
						<Field label="Endpoint URL">
							<Input
								id="webhook-url"
								type="url"
								value={webhookUrl}
								onChange={(e) => setWebhookUrl(e.target.value)}
								placeholder="https://example.com/webhook"
								required
							/>
						</Field>

						{config.secret && (
							<p className="text-[13px] text-ink3-light dark:text-ink3-dark">
								Current secret: <span className="font-mono">••••••••</span>
							</p>
						)}

						<div className="flex flex-wrap gap-2">
							<Button type="submit" disabled={isUpdating}>
								{isUpdating ? "Saving…" : "Save endpoint"}
							</Button>
							<Button
								type="button"
								variant="secondary"
								onClick={handleRegenerateSecret}
								disabled={isRegenerating || !config.webhookUrl}
							>
								{isRegenerating ? "Regenerating…" : "Regenerate secret"}
							</Button>
						</div>

						{showSecret && (
							<div className="space-y-2 rounded-lg border border-status-warning-soft-light bg-status-warning-soft-light p-3 dark:border-[#D3932B]/25 dark:bg-[#D3932B]/10">
								<p className="text-[13px] font-medium text-status-warning-ink-light dark:text-[#F0C468]">
									New secret — shown once. Copy it now.
								</p>
								<div className="flex items-center gap-2 rounded-lg border border-line-light bg-card-light p-2 dark:border-line-dark dark:bg-card-dark">
									<code className="min-w-0 flex-1 break-all px-1 font-mono text-[13px] text-ink-light dark:text-ink-dark">
										{showSecret}
									</code>
									<Button size="sm" onClick={() => copyToClipboard(showSecret)}>
										Copy
									</Button>
								</div>
							</div>
						)}

						<div className="flex items-start gap-2 pt-1 text-[12.5px] text-ink3-light dark:text-ink3-dark">
							<Webhook className="mt-0.5 size-4 shrink-0" />
							Signing lets your server confirm a request really came from Vantage.
						</div>
					</form>
				</Card>
			)}
		</div>
	);
}
