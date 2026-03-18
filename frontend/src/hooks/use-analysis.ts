import { useProjectContext } from "@/contexts/project-context";
import { apiFetch } from "@/lib/api-client";
import type {
  CycleResponse,
  LinkType,
  RelationValidationResponse,
  RequirementSummaryResponse,
} from "@/types/api";
import { useQuery } from "@tanstack/react-query";

export function useCycles() {
  const { activeProject } = useProjectContext();
  return useQuery({
    queryKey: ["analysis", "cycles", activeProject?.identifier],
    queryFn: () =>
      apiFetch<CycleResponse[]>("/analysis/cycles", {
        params: { project: activeProject?.identifier },
      }),
    enabled: !!activeProject,
  });
}

export function useOrphans() {
  const { activeProject } = useProjectContext();
  return useQuery({
    queryKey: ["analysis", "orphans", activeProject?.identifier],
    queryFn: () =>
      apiFetch<RequirementSummaryResponse[]>("/analysis/orphans", {
        params: { project: activeProject?.identifier },
      }),
    enabled: !!activeProject,
  });
}

export function useCoverageGaps(linkType: LinkType) {
  const { activeProject } = useProjectContext();
  return useQuery({
    queryKey: [
      "analysis",
      "coverage-gaps",
      linkType,
      activeProject?.identifier,
    ],
    queryFn: () =>
      apiFetch<RequirementSummaryResponse[]>("/analysis/coverage-gaps", {
        params: { linkType, project: activeProject?.identifier },
      }),
    enabled: !!activeProject,
  });
}

export function useImpact(id: string | undefined) {
  return useQuery({
    queryKey: ["analysis", "impact", id],
    queryFn: () =>
      apiFetch<RequirementSummaryResponse[]>(`/analysis/impact/${id}`),
    enabled: !!id,
  });
}

export function useCrossWave() {
  const { activeProject } = useProjectContext();
  return useQuery({
    queryKey: ["analysis", "cross-wave", activeProject?.identifier],
    queryFn: () =>
      apiFetch<RelationValidationResponse[]>("/analysis/cross-wave", {
        params: { project: activeProject?.identifier },
      }),
    enabled: !!activeProject,
  });
}
