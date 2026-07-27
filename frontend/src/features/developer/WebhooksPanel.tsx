import { useCallback, useState } from "react";
import toast from "react-hot-toast";
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
				toast.success("Webhook updated successfully");
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
			.then(() => {
				toast.success("Copied to clipboard");
			})
			.catch(() => {
				toast.error("Failed to copy");
			});
	}, []);

	return (
		<div>
			<h3 className="text-lg font-semibold mb-4">Webhook Configuration</h3>
			{isLoading && <div className="text-gray-500">Loading webhook configuration...</div>}
			{!isLoading && (
				<form onSubmit={handleUpdate} className="space-y-4">
					<div>
						<label htmlFor="webhook-url" className="block text-sm font-medium mb-1">
							Webhook URL
						</label>
						<input
							id="webhook-url"
							type="url"
							value={webhookUrl}
							onChange={(e) => setWebhookUrl(e.target.value)}
							className="w-full px-3 py-2 border border-gray-300 rounded"
							placeholder="https://example.com/webhook"
							required
						/>
					</div>
					<div className="flex gap-2">
						<button
							type="submit"
							disabled={isUpdating}
							className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
						>
							{isUpdating ? "Updating..." : "Update Webhook"}
						</button>
						<button
							type="button"
							onClick={handleRegenerateSecret}
							disabled={isRegenerating || !config.webhookUrl}
							className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 disabled:opacity-50"
						>
							{isRegenerating ? "Regenerating..." : "Regenerate Secret"}
						</button>
					</div>
					{config.secret && (
						<div className="mt-2 text-sm text-gray-600 dark:text-gray-400">
							Current secret: <span className="font-mono">••••••••</span>
						</div>
					)}
					{showSecret && (
						<div className="mt-4 bg-yellow-50 border border-yellow-200 p-3 rounded">
							<p className="text-sm text-yellow-800 font-semibold">
								New webhook secret generated. This will not be shown again.
							</p>
							<div className="flex items-center gap-2 mt-2 bg-white p-2 rounded">
								<code className="flex-1 font-mono text-sm break-all">{showSecret}</code>
								<button
									type="button"
									onClick={() => copyToClipboard(showSecret)}
									className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
								>
									Copy
								</button>
							</div>
						</div>
					)}
				</form>
			)}
		</div>
	);
}
