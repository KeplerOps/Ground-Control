import { apiDelete, apiFetch } from "@/lib/api-client";
import { queryClient } from "@/lib/query-client";
import type { RelationRequest, RelationResponse } from "@/types/api";
import { useMutation, useQuery } from "@tanstack/react-query";

export function useRelations(requirementId: string | undefined) {
  return useQuery({
    queryKey: ["relations", requirementId],
    queryFn: () =>
      apiFetch<RelationResponse[]>(`/requirements/${requirementId}/relations`),
    enabled: !!requirementId,
  });
}

export function useCreateRelation(requirementId: string) {
  return useMutation({
    mutationFn: (data: RelationRequest) =>
      apiFetch<RelationResponse>(`/requirements/${requirementId}/relations`, {
        method: "POST",
        body: data,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["relations", requirementId],
      });
    },
  });
}

export function useDeleteRelation(requirementId: string) {
  return useMutation({
    mutationFn: (relationId: string) =>
      apiDelete(`/requirements/${requirementId}/relations/${relationId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["relations", requirementId],
      });
    },
  });
}
