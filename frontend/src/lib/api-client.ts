const BASE = "/api/v1";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body?.error?.message ?? `HTTP ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  // Workspaces
  listWorkspaces: () =>
    request<import("@/types/api").Workspace[]>("/workspaces"),
  createWorkspace: (data: {
    identifier: string;
    name: string;
    description?: string;
  }) =>
    request<import("@/types/api").Workspace>("/workspaces", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Workflows
  listWorkflows: (workspace?: string, page = 0, size = 20) =>
    request<import("@/types/api").Page<import("@/types/api").Workflow>>(
      `/workflows?workspace=${workspace ?? ""}&page=${page}&size=${size}&sort=updatedAt,desc`,
    ),
  getWorkflow: (id: string) =>
    request<import("@/types/api").Workflow>(`/workflows/${id}`),
  createWorkflow: (
    data: { name: string; description?: string; tags?: string },
    workspace?: string,
  ) =>
    request<import("@/types/api").Workflow>(
      `/workflows?workspace=${workspace ?? ""}`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),
  updateWorkflow: (id: string, data: Record<string, unknown>) =>
    request<import("@/types/api").Workflow>(`/workflows/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deleteWorkflow: (id: string) =>
    request<void>(`/workflows/${id}`, { method: "DELETE" }),
  publishWorkflow: (id: string) =>
    request<import("@/types/api").Workflow>(`/workflows/${id}/publish`, {
      method: "POST",
    }),
  transitionWorkflow: (id: string, status: string) =>
    request<import("@/types/api").Workflow>(`/workflows/${id}/transition`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),
  validateWorkflow: (id: string) =>
    request<import("@/types/api").ValidationResult>(
      `/workflows/${id}/validate`,
      { method: "POST" },
    ),

  // Nodes
  getNodes: (workflowId: string) =>
    request<import("@/types/api").WorkflowNode[]>(
      `/workflows/${workflowId}/nodes`,
    ),
  addNode: (workflowId: string, data: Record<string, unknown>) =>
    request<import("@/types/api").WorkflowNode>(
      `/workflows/${workflowId}/nodes`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),
  updateNode: (
    workflowId: string,
    nodeId: string,
    data: Record<string, unknown>,
  ) =>
    request<import("@/types/api").WorkflowNode>(
      `/workflows/${workflowId}/nodes/${nodeId}`,
      {
        method: "PUT",
        body: JSON.stringify(data),
      },
    ),
  deleteNode: (workflowId: string, nodeId: string) =>
    request<void>(`/workflows/${workflowId}/nodes/${nodeId}`, {
      method: "DELETE",
    }),

  // Edges
  getEdges: (workflowId: string) =>
    request<import("@/types/api").WorkflowEdge[]>(
      `/workflows/${workflowId}/edges`,
    ),
  addEdge: (
    workflowId: string,
    data: {
      sourceNodeId: string;
      targetNodeId: string;
      conditionExpr?: string;
      label?: string;
    },
  ) =>
    request<import("@/types/api").WorkflowEdge>(
      `/workflows/${workflowId}/edges`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),
  deleteEdge: (workflowId: string, edgeId: string) =>
    request<void>(`/workflows/${workflowId}/edges/${edgeId}`, {
      method: "DELETE",
    }),

  // Executions
  executeWorkflow: (workflowId: string, inputs?: string) =>
    request<import("@/types/api").Execution>(
      `/workflows/${workflowId}/execute`,
      {
        method: "POST",
        body: JSON.stringify({ inputs: inputs ?? "{}" }),
      },
    ),
  listExecutions: (workspace?: string, page = 0, size = 20) =>
    request<import("@/types/api").Page<import("@/types/api").Execution>>(
      `/executions?workspace=${workspace ?? ""}&page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  listWorkflowExecutions: (workflowId: string, page = 0, size = 20) =>
    request<import("@/types/api").Page<import("@/types/api").Execution>>(
      `/workflows/${workflowId}/executions?page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  getExecution: (id: string) =>
    request<import("@/types/api").ExecutionDetail>(`/executions/${id}`),
  cancelExecution: (id: string) =>
    request<import("@/types/api").Execution>(`/executions/${id}/cancel`, {
      method: "POST",
    }),
  retryExecution: (id: string) =>
    request<import("@/types/api").Execution>(`/executions/${id}/retry`, {
      method: "POST",
    }),
  getExecutionStats: (workflowId: string) =>
    request<import("@/types/api").ExecutionStats>(
      `/workflows/${workflowId}/executions/stats`,
    ),

  // Triggers
  listTriggers: (workflowId: string) =>
    request<import("@/types/api").Trigger[]>(
      `/triggers?workflowId=${workflowId}`,
    ),
  createTrigger: (data: {
    workflowId: string;
    name: string;
    triggerType: string;
    config?: string;
  }) =>
    request<import("@/types/api").Trigger>("/triggers", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  updateTrigger: (id: string, data: Record<string, unknown>) =>
    request<import("@/types/api").Trigger>(`/triggers/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  toggleTrigger: (id: string) =>
    request<import("@/types/api").Trigger>(`/triggers/${id}/toggle`, {
      method: "POST",
    }),
  deleteTrigger: (id: string) =>
    request<void>(`/triggers/${id}`, { method: "DELETE" }),

  // Credentials
  listCredentials: (workspace?: string) =>
    request<import("@/types/api").Credential[]>(
      `/credentials?workspace=${workspace ?? ""}`,
    ),
  createCredential: (
    data: { name: string; credentialType: string; data?: string },
    workspace?: string,
  ) =>
    request<import("@/types/api").Credential>(
      `/credentials?workspace=${workspace ?? ""}`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),
  deleteCredential: (id: string) =>
    request<void>(`/credentials/${id}`, { method: "DELETE" }),

  // Variables
  listVariables: (workspace?: string) =>
    request<import("@/types/api").Variable[]>(
      `/variables?workspace=${workspace ?? ""}`,
    ),
  createVariable: (
    data: {
      key: string;
      value?: string;
      description?: string;
      secret?: boolean;
    },
    workspace?: string,
  ) =>
    request<import("@/types/api").Variable>(
      `/variables?workspace=${workspace ?? ""}`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),
  updateVariable: (id: string, data: Record<string, unknown>) =>
    request<import("@/types/api").Variable>(`/variables/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deleteVariable: (id: string) =>
    request<void>(`/variables/${id}`, { method: "DELETE" }),
};
