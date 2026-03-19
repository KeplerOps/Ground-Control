import { apiDelete, apiFetch } from "@/lib/api-client";
import { queryClient } from "@/lib/query-client";
import type {
  TraceabilityLinkRequest,
  TraceabilityLinkResponse,
} from "@/types/api";
import { useMutation, useQuery } from "@tanstack/react-query";

export function useTraceabilityLinks(requirementId: string | undefined) {
  return useQuery({
    queryKey: ["traceability", requirementId],
    queryFn: () =>
      apiFetch<TraceabilityLinkResponse[]>(
        `/requirements/${requirementId}/traceability`,
      ),
    enabled: !!requirementId,
  });
}

export function useCreateTraceabilityLink(requirementId: string) {
  return useMutation({
    mutationFn: (data: TraceabilityLinkRequest) =>
      apiFetch<TraceabilityLinkResponse>(
        `/requirements/${requirementId}/traceability`,
        { method: "POST", body: data },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["traceability", requirementId],
      });
    },
  });
}

export function useDeleteTraceabilityLink(requirementId: string) {
  return useMutation({
    mutationFn: (linkId: string) =>
      apiDelete(`/requirements/${requirementId}/traceability/${linkId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["traceability", requirementId],
      });
    },
  });
}
