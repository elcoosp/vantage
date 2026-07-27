import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "../../lib/api";

export interface WebhookConfig {
	webhookUrl: string;
	secret?: string | null;
}

async function fetchWebhookConfig(): Promise<WebhookConfig> {
	return { webhookUrl: "", secret: null };
}

async function updateWebhookUrl(webhookUrl: string): Promise<{ secret: string }> {
	const res = await apiClient.put<{ secret: string }>("/webhooks", { webhookUrl });
	return res.data;
}

export function useWebhooks() {
	const queryClient = useQueryClient();

	const { data, isLoading, error } = useQuery({
		queryKey: ["webhookConfig"],
		queryFn: fetchWebhookConfig,
		staleTime: 60000,
	});

	const updateMutation = useMutation({
		mutationFn: updateWebhookUrl,
		onSuccess: (result, variables) => {
			queryClient.setQueryData(["webhookConfig"], {
				webhookUrl: variables,
				secret: result.secret,
			});
			queryClient.invalidateQueries({ queryKey: ["webhookConfig"] });
		},
	});

	const regenerateSecret = async () => {
		if (!data?.webhookUrl) {
			throw new Error("No webhook URL set");
		}
		return updateMutation.mutateAsync(data.webhookUrl);
	};

	return {
		config: data ?? { webhookUrl: "", secret: null },
		isLoading,
		error,
		updateWebhook: updateMutation.mutateAsync,
		isUpdating: updateMutation.isPending,
		regenerateSecret,
		isRegenerating: updateMutation.isPending,
	};
}
