import { api } from "@/lib/api-client";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export function useExecutions(workspace?: string) {
  return useQuery({
    queryKey: ["executions", workspace],
    queryFn: () => api.listExecutions(workspace),
  });
}

export function useExecution(id: string) {
  return useQuery({
    queryKey: ["execution", id],
    queryFn: () => api.getExecution(id),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "RUNNING" || status === "PENDING" || status === "QUEUED"
        ? 2000
        : false;
    },
  });
}

export function useCancelExecution() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.cancelExecution(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["execution", id] });
      qc.invalidateQueries({ queryKey: ["executions"] });
    },
  });
}

export function useRetryExecution() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.retryExecution(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["executions"] }),
  });
}
