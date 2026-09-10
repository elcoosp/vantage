import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "../../lib/api";

async function fetchChaosMonkeyStatus(): Promise<boolean> {
	const response = await apiClient.get<boolean>("/admin/chaos-monkey/payment-failure");
	return response.data;
}

async function toggleChaosMonkey(enabled: boolean): Promise<void> {
	await apiClient.post("/admin/chaos-monkey/payment-failure", { enabled });
}

export function useChaosMonkey() {
	const queryClient = useQueryClient();

	const statusQuery = useQuery({
		queryKey: ["chaosMonkeyStatus"],
		queryFn: fetchChaosMonkeyStatus,
		staleTime: 2000,
		retry: false,
	});

	const toggleMutation = useMutation({
		mutationFn: toggleChaosMonkey,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ["chaosMonkeyStatus"] });
		},
	});

	return {
		isEnabled: statusQuery.data ?? false,
		isLoading: statusQuery.isLoading,
		isError: statusQuery.isError,
		toggle: (enabled: boolean) => toggleMutation.mutate(enabled),
		isToggling: toggleMutation.isPending,
	};
}
