import { useProjectContext } from "@/contexts/project-context";
import { apiFetch } from "@/lib/api-client";
import { queryClient } from "@/lib/query-client";
import type {
  BulkStatusTransitionResponse,
  CloneRequirementRequest,
  PagedResponse,
  RequirementRequest,
  RequirementResponse,
  Status,
  UpdateRequirementRequest,
} from "@/types/api";
import { useMutation, useQuery } from "@tanstack/react-query";

interface RequirementFilters {
  status?: string;
  type?: string;
  priority?: string;
  wave?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export function useRequirements(filters: RequirementFilters = {}) {
  const { activeProject } = useProjectContext();
  const {
    status,
    type,
    priority,
    wave,
    search,
    page = 0,
    size = 25,
    sort,
  } = filters;

  return useQuery({
    queryKey: ["requirements", activeProject?.identifier, filters],
    queryFn: () =>
      apiFetch<PagedResponse<RequirementResponse>>("/requirements", {
        params: {
          project: activeProject?.identifier,
          status,
          type,
          priority,
          wave,
          search,
          page: String(page),
          size: String(size),
          sort,
        },
      }),
    enabled: !!activeProject,
  });
}

export function useRequirement(id: string | undefined) {
  return useQuery({
    queryKey: ["requirement", id],
    queryFn: () => apiFetch<RequirementResponse>(`/requirements/${id}`),
    enabled: !!id,
  });
}

export function useCreateRequirement() {
  const { activeProject } = useProjectContext();

  return useMutation({
    mutationFn: (data: RequirementRequest) =>
      apiFetch<RequirementResponse>("/requirements", {
        method: "POST",
        body: data,
        params: { project: activeProject?.identifier },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
    },
  });
}

export function useUpdateRequirement(id: string) {
  return useMutation({
    mutationFn: (data: UpdateRequirementRequest) =>
      apiFetch<RequirementResponse>(`/requirements/${id}`, {
        method: "PUT",
        body: data,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
      queryClient.invalidateQueries({ queryKey: ["requirement", id] });
    },
  });
}

export function useTransitionStatus(id: string) {
  return useMutation({
    mutationFn: (status: Status) =>
      apiFetch<RequirementResponse>(`/requirements/${id}/transition`, {
        method: "POST",
        body: { status },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
      queryClient.invalidateQueries({ queryKey: ["requirement", id] });
    },
  });
}

export function useBulkTransition() {
  return useMutation({
    mutationFn: (data: { ids: string[]; status: Status }) =>
      apiFetch<BulkStatusTransitionResponse>("/requirements/bulk/transition", {
        method: "POST",
        body: data,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
    },
  });
}

export function useCloneRequirement(id: string) {
  return useMutation({
    mutationFn: (data: CloneRequirementRequest) =>
      apiFetch<RequirementResponse>(`/requirements/${id}/clone`, {
        method: "POST",
        body: data,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
    },
  });
}

export function useArchiveRequirement(id: string) {
  return useMutation({
    mutationFn: () =>
      apiFetch<RequirementResponse>(`/requirements/${id}/archive`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
      queryClient.invalidateQueries({ queryKey: ["requirement", id] });
    },
  });
}
