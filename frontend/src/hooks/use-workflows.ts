import { api } from "@/lib/api-client";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export function useWorkflows(workspace?: string) {
  return useQuery({
    queryKey: ["workflows", workspace],
    queryFn: () => api.listWorkflows(workspace),
  });
}

export function useWorkflow(id: string) {
  return useQuery({
    queryKey: ["workflow", id],
    queryFn: () => api.getWorkflow(id),
    enabled: !!id,
  });
}

export function useWorkflowNodes(workflowId: string) {
  return useQuery({
    queryKey: ["workflow-nodes", workflowId],
    queryFn: () => api.getNodes(workflowId),
    enabled: !!workflowId,
  });
}

export function useWorkflowEdges(workflowId: string) {
  return useQuery({
    queryKey: ["workflow-edges", workflowId],
    queryFn: () => api.getEdges(workflowId),
    enabled: !!workflowId,
  });
}

export function useCreateWorkflow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: {
      name: string;
      description?: string;
      tags?: string;
      workspace?: string;
    }) => api.createWorkflow(data, data.workspace),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["workflows"] }),
  });
}

export function useDeleteWorkflow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteWorkflow(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["workflows"] }),
  });
}

export function usePublishWorkflow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.publishWorkflow(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["workflow", id] });
      qc.invalidateQueries({ queryKey: ["workflows"] });
    },
  });
}

export function useExecuteWorkflow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      workflowId,
      inputs,
    }: {
      workflowId: string;
      inputs?: string;
    }) => api.executeWorkflow(workflowId, inputs),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["executions"] }),
  });
}
