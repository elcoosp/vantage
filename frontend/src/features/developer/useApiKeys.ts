import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "../../lib/api";

export interface ApiKey {
	id: string;
	name: string;
	key?: string | null;
	prefix: string;
	createdAt: string;
	lastUsedAt?: string | null;
}

interface GenerateApiKeyRequest {
	name: string;
}

async function fetchApiKeys(): Promise<ApiKey[]> {
	const res = await apiClient.get<ApiKey[]>("/api-keys");
	return res.data;
}

async function generateApiKey(data: GenerateApiKeyRequest): Promise<ApiKey> {
	const res = await apiClient.post<ApiKey>("/api-keys", data);
	return res.data;
}

async function revokeApiKey(id: string): Promise<void> {
	await apiClient.delete(`/api-keys/${id}`);
}

export function useApiKeys() {
	const queryClient = useQueryClient();

	const { data, isLoading, error } = useQuery({
		queryKey: ["apiKeys"],
		queryFn: fetchApiKeys,
	});

	const generateMutation = useMutation({
		mutationFn: generateApiKey,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["apiKeys"] });
		},
	});

	const revokeMutation = useMutation({
		mutationFn: revokeApiKey,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["apiKeys"] });
		},
	});

	return {
		keys: data ?? [],
		isLoading,
		error,
		generate: generateMutation.mutateAsync,
		isGenerating: generateMutation.isPending,
		revoke: revokeMutation.mutateAsync,
		isRevoking: revokeMutation.isPending,
	};
}
