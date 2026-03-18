import { apiFetch } from "@/lib/api-client";
import { useQuery } from "@tanstack/react-query";

export interface ProjectResponse {
  id: string;
  identifier: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export function useProjects() {
  return useQuery({
    queryKey: ["projects"],
    queryFn: () => apiFetch<ProjectResponse[]>("/projects"),
  });
}
