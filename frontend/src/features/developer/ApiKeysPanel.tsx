import { useState } from "react";
import toast from "react-hot-toast";
import { useApiKeys } from "./useApiKeys";

export function ApiKeysPanel() {
	const { keys, isLoading, generate, isGenerating, revoke } = useApiKeys();
	const [showGenerateModal, setShowGenerateModal] = useState(false);
	const [keyName, setKeyName] = useState("");
	const [generatedKey, setGeneratedKey] = useState<string | null>(null);
	const [revokingIds, setRevokingIds] = useState<Set<string>>(new Set());

	const handleGenerate = async () => {
		if (!keyName.trim()) {
			toast.error("Please enter a key name");
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
		if (!window.confirm("Are you sure you want to revoke this API key? This action cannot be undone.")) return;
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
			.then(() => {
				toast.success("Copied to clipboard");
			})
			.catch(() => {
				toast.error("Failed to copy");
			});
	};

	return (
		<div>
			<div className="flex justify-between items-center mb-4">
				<h3 className="text-lg font-semibold">API Keys</h3>
				<button
					type="button"
					onClick={() => setShowGenerateModal(true)}
					className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
				>
					Generate New Key
				</button>
			</div>

			{isLoading && <div className="text-gray-500">Loading API keys...</div>}
			{!isLoading && keys.length === 0 && <div className="text-gray-500">No API keys created yet.</div>}

			{keys.length > 0 && (
				<table className="min-w-full bg-white dark:bg-gray-800 shadow rounded-lg">
					<thead>
						<tr className="border-b border-gray-200 dark:border-gray-700">
							<th className="px-4 py-2 text-left text-sm font-medium">Name</th>
							<th className="px-4 py-2 text-left text-sm font-medium">Key Prefix</th>
							<th className="px-4 py-2 text-left text-sm font-medium">Last Used</th>
							<th className="px-4 py-2 text-left text-sm font-medium">Actions</th>
						</tr>
					</thead>
					<tbody>
						{keys.map((key) => (
							<tr key={key.id} className="border-b border-gray-100 dark:border-gray-700">
								<td className="px-4 py-2 text-sm">{key.name}</td>
								<td className="px-4 py-2 text-sm font-mono">{key.prefix}</td>
								<td className="px-4 py-2 text-sm text-gray-500">
									{key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleString() : "Never"}
								</td>
								<td className="px-4 py-2">
									<button
										type="button"
										onClick={() => handleRevoke(key.id)}
										disabled={revokingIds.has(key.id)}
										className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
									>
										Revoke
									</button>
								</td>
							</tr>
						))}
					</tbody>
				</table>
			)}

			{/* Generate Modal */}
			{showGenerateModal && (
				<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
					<div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
						{generatedKey ? (
							<div>
								<h3 className="text-xl font-bold mb-2 text-green-600">Key Generated</h3>
								<div className="bg-yellow-50 border border-yellow-200 p-3 rounded mb-4">
									<p className="text-sm text-yellow-800 font-semibold">
										This key will not be shown again. Please store it securely.
									</p>
								</div>
								<div className="flex items-center gap-2 bg-gray-100 dark:bg-gray-700 p-2 rounded">
									<code className="flex-1 font-mono text-sm break-all">{generatedKey}</code>
									<button
										type="button"
										onClick={() => copyToClipboard(generatedKey)}
										className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
									>
										Copy
									</button>
								</div>
								<div className="mt-4 flex justify-end">
									<button
										type="button"
										onClick={closeGenerateModal}
										className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
									>
										Done
									</button>
								</div>
							</div>
						) : (
							<div>
								<h3 className="text-xl font-bold mb-4">Generate New API Key</h3>
								<div className="mb-3">
									<label htmlFor="key-name" className="block text-sm font-medium mb-1">
										Key Name
									</label>
									<input
										id="key-name"
										type="text"
										value={keyName}
										onChange={(e) => setKeyName(e.target.value)}
										className="w-full px-3 py-2 border border-gray-300 rounded"
										placeholder="e.g., Production Key"
									/>
								</div>
								<div className="flex justify-end gap-2">
									<button
										type="button"
										onClick={closeGenerateModal}
										className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
										disabled={isGenerating}
									>
										Cancel
									</button>
									<button
										type="button"
										onClick={handleGenerate}
										disabled={isGenerating || !keyName.trim()}
										className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
									>
										{isGenerating ? "Generating..." : "Generate"}
									</button>
								</div>
							</div>
						)}
					</div>
				</div>
			)}
		</div>
	);
}
