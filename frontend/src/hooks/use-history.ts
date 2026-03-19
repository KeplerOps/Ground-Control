import { apiFetch } from "@/lib/api-client";
import type {
  RelationHistoryResponse,
  RequirementHistoryResponse,
  TraceabilityLinkHistoryResponse,
} from "@/types/api";
import { useQuery } from "@tanstack/react-query";

export function useRequirementHistory(requirementId: string | undefined) {
  return useQuery({
    queryKey: ["history", "requirement", requirementId],
    queryFn: () =>
      apiFetch<RequirementHistoryResponse[]>(
        `/requirements/${requirementId}/history`,
      ),
    enabled: !!requirementId,
  });
}

export function useRelationHistory(
  requirementId: string | undefined,
  relationId: string | undefined,
) {
  return useQuery({
    queryKey: ["history", "relation", requirementId, relationId],
    queryFn: () =>
      apiFetch<RelationHistoryResponse[]>(
        `/requirements/${requirementId}/relations/${relationId}/history`,
      ),
    enabled: !!requirementId && !!relationId,
  });
}

export function useTraceabilityLinkHistory(
  requirementId: string | undefined,
  linkId: string | undefined,
) {
  return useQuery({
    queryKey: ["history", "traceability", requirementId, linkId],
    queryFn: () =>
      apiFetch<TraceabilityLinkHistoryResponse[]>(
        `/requirements/${requirementId}/traceability/${linkId}/history`,
      ),
    enabled: !!requirementId && !!linkId,
  });
}
