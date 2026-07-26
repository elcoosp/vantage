import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

async function fetchChaosMonkeyStatus(): Promise<boolean> {
	const response = await axios.get<boolean>("/api/v1/admin/chaos-monkey/payment-failure");
	return response.data;
}

async function toggleChaosMonkey(enabled: boolean): Promise<void> {
	await axios.post("/api/v1/admin/chaos-monkey/payment-failure", { enabled });
}

export function useChaosMonkey() {
	const queryClient = useQueryClient();

	const statusQuery = useQuery({
		queryKey: ["chaosMonkeyStatus"],
		queryFn: fetchChaosMonkeyStatus,
		staleTime: 2000,
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
