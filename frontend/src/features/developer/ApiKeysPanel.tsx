import { KeyRound, Plus } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Button, Card, Field, Input, Modal, THead, Table, Td, Th, Tr } from "../../components/ui";
import { useApiKeys } from "./useApiKeys";

export function ApiKeysPanel() {
	const { keys, isLoading, generate, isGenerating, revoke } = useApiKeys();
	const [showGenerateModal, setShowGenerateModal] = useState(false);
	const [keyName, setKeyName] = useState("");
	const [generatedKey, setGeneratedKey] = useState<string | null>(null);
	const [revokingIds, setRevokingIds] = useState<Set<string>>(new Set());

	const handleGenerate = async () => {
		if (!keyName.trim()) {
			toast.error("Enter a key name first");
			return;
		}
		try {
			const newKey = await generate({ name: keyName.trim() });
			setGeneratedKey(newKey.key ?? null);
			setKeyName("");
		} catch (err) {
			toast.error("Failed to generate API key");
		}
	};

	const handleRevoke = async (id: string) => {
		if (!window.confirm("Revoke this API key? This cannot be undone.")) return;
		setRevokingIds((prev) => new Set(prev).add(id));
		try {
			await revoke(id);
			toast.success("API key revoked");
		} catch (err) {
			toast.error("Failed to revoke API key");
		} finally {
			setRevokingIds((prev) => {
				const next = new Set(prev);
				next.delete(id);
				return next;
			});
		}
	};

	const closeGenerateModal = () => {
		setShowGenerateModal(false);
		setKeyName("");
		setGeneratedKey(null);
	};

	const copyToClipboard = (text: string) => {
		navigator.clipboard
			.writeText(text)
			.then(() => toast.success("Copied to clipboard"))
			.catch(() => toast.error("Failed to copy"));
	};

	return (
		<div className="space-y-4">
			<div className="flex items-center justify-between">
				<p className="text-[13px] text-ink3-light dark:text-ink3-dark">
					Keys let your integrations authenticate against the Vantage API.
				</p>
				<Button size="sm" onClick={() => setShowGenerateModal(true)}>
					<Plus className="size-4" />
					New key
				</Button>
			</div>

			{isLoading && (
				<Card>
					<div className="space-y-2 p-5">
						{Array.from({ length: 3 }).map((_, i) => (
							// biome-ignore lint/suspicious/noArrayIndexKey: static skeleton rows
							<div key={i} className="h-8 animate-pulse rounded-md bg-card2-light dark:bg-card2-dark" />
						))}
					</div>
				</Card>
			)}

			{!isLoading && keys.length === 0 && (
				<Card>
					<div className="flex flex-col items-center px-6 py-14 text-center">
						<div className="mb-4 flex size-12 items-center justify-center rounded-xl bg-card2-light text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
							<KeyRound className="size-5" />
						</div>
						<h3 className="text-[15px] font-semibold text-ink-light dark:text-ink-dark">No API keys yet</h3>
						<p className="mt-1.5 max-w-sm text-sm text-ink3-light dark:text-ink3-dark">
							Generate a key to start building against Vantage.
						</p>
						<Button size="sm" className="mt-5" onClick={() => setShowGenerateModal(true)}>
							<Plus className="size-4" />
							New key
						</Button>
					</div>
				</Card>
			)}

			{keys.length > 0 && (
				<Card>
					<Table>
						<THead>
							<Th>Name</Th>
							<Th>Key prefix</Th>
							<Th>Last used</Th>
							<Th className="text-right">Action</Th>
						</THead>
						<tbody>
							{keys.map((key) => (
								<Tr key={key.id}>
									<Td className="font-medium text-ink-light dark:text-ink-dark">{key.name}</Td>
									<Td className="font-mono text-[12px] text-ink3-light dark:text-ink3-dark">{key.prefix}</Td>
									<Td className="text-[13px] text-ink3-light dark:text-ink3-dark">
										{key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleString() : "Never"}
									</Td>
									<Td className="text-right">
										<Button
											variant="danger"
											size="sm"
											onClick={() => handleRevoke(key.id)}
											disabled={revokingIds.has(key.id)}
										>
											{revokingIds.has(key.id) ? "Revoking…" : "Revoke"}
										</Button>
									</Td>
								</Tr>
							))}
						</tbody>
					</Table>
				</Card>
			)}

			<Modal
				open={showGenerateModal}
				onClose={closeGenerateModal}
				title={generatedKey ? "Key generated" : "Generate API key"}
				description={
					generatedKey ? "Store this now — it won't be shown again." : "Create a credential for your integration."
				}
			>
				{generatedKey ? (
					<div className="space-y-4">
						<div className="rounded-lg border border-status-warning-soft-light bg-status-warning-soft-light px-3 py-2.5 dark:border-[#D3932B]/25 dark:bg-[#D3932B]/10">
							<p className="text-[13px] text-status-warning-ink-light dark:text-[#F0C468]">
								<strong>Keep it secret.</strong> Treat this like a password.
							</p>
						</div>
						<div className="flex items-center gap-2 rounded-lg border border-line-light bg-card2-light p-2 dark:border-line-dark dark:bg-card2-dark">
							<code className="min-w-0 flex-1 break-all px-1 font-mono text-[13px] text-ink-light dark:text-ink-dark">
								{generatedKey}
							</code>
							<Button size="sm" onClick={() => copyToClipboard(generatedKey)}>
								Copy
							</Button>
						</div>
						<div className="flex justify-end">
							<Button variant="secondary" onClick={closeGenerateModal}>
								Done
							</Button>
						</div>
					</div>
				) : (
					<div className="space-y-4">
						<Field label="Key name">
							<Input
								id="key-name"
								type="text"
								value={keyName}
								onChange={(e) => setKeyName(e.target.value)}
								placeholder="e.g. Production integration"
								autoFocus
							/>
						</Field>
						<div className="flex justify-end gap-2">
							<Button variant="secondary" onClick={closeGenerateModal} disabled={isGenerating}>
								Cancel
							</Button>
							<Button onClick={handleGenerate} disabled={isGenerating || !keyName.trim()}>
								{isGenerating ? "Generating…" : "Generate key"}
							</Button>
						</div>
					</div>
				)}
			</Modal>
		</div>
	);
}
