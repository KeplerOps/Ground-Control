// ---------------------------------------------------------------------------
// Constants (matching Java enums)
// ---------------------------------------------------------------------------

export const WORKFLOW_STATUSES = ["DRAFT", "ACTIVE", "PAUSED", "ARCHIVED"];
export const NODE_TYPES = [
  "TRIGGER",
  "ACTION",
  "CONDITION",
  "DELAY",
  "LOOP",
  "TRANSFORM",
  "HTTP_REQUEST",
  "CODE",
  "SUB_WORKFLOW",
];
export const TRIGGER_TYPES = ["WEBHOOK", "SCHEDULE", "EVENT", "MANUAL"];

// ---------------------------------------------------------------------------
// Field name mapping (snake_case MCP <-> camelCase API)
// ---------------------------------------------------------------------------

const TO_CAMEL = {
  node_type: "nodeType",
  source_node_id: "sourceNodeId",
  target_node_id: "targetNodeId",
  workflow_id: "workflowId",
  workspace_id: "workspaceId",
  trigger_type: "triggerType",
  credential_type: "credentialType",
  encrypted_data: "encryptedData",
  created_at: "createdAt",
  updated_at: "updatedAt",
  started_at: "startedAt",
  completed_at: "completedAt",
  position_x: "positionX",
  position_y: "positionY",
  retry_count: "retryCount",
  max_retries: "maxRetries",
  timeout_seconds: "timeoutSeconds",
  is_active: "isActive",
  cron_expression: "cronExpression",
  next_fire_time: "nextFireTime",
  last_fired_at: "lastFiredAt",
  execution_count: "executionCount",
  variable_type: "variableType",
  is_secret: "isSecret",
};

const TO_SNAKE = Object.fromEntries(Object.entries(TO_CAMEL).map(([k, v]) => [v, k]));

function toCamelCase(obj) {
  if (obj === null || obj === undefined || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(toCamelCase);
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    out[TO_CAMEL[k] || k] = toCamelCase(v);
  }
  return out;
}

function toSnakeCase(obj) {
  if (obj === null || obj === undefined || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(toSnakeCase);
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    out[TO_SNAKE[k] || k] = toSnakeCase(v);
  }
  return out;
}

// ---------------------------------------------------------------------------
// HTTP client
// ---------------------------------------------------------------------------

const BASE_URL = process.env.GC_BASE_URL || "http://localhost:8000";

export function buildUrl(path, params) {
  const url = new URL(path, BASE_URL);
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined && v !== null && v !== "") {
        url.searchParams.set(k, String(v));
      }
    }
  }
  return url.toString();
}

export function parseErrorBody(text) {
  try {
    const body = JSON.parse(text);
    if (body.error && body.error.message) {
      return body.error.message;
    }
    return text;
  } catch {
    return text;
  }
}

async function request(method, path, { body, params } = {}) {
  const url = buildUrl(path, params);
  const options = { method, headers: { "X-Actor": "mcp-server" } };

  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(toCamelCase(body));
  }

  const res = await fetch(url, options);

  if (res.status === 204) return null;

  const text = await res.text();

  if (!res.ok) {
    const msg = parseErrorBody(text);
    throw new Error(`${res.status}: ${msg}`);
  }

  const data = text ? JSON.parse(text) : null;
  return toSnakeCase(data);
}

// ---------------------------------------------------------------------------
// Workspace API
// ---------------------------------------------------------------------------

export async function createWorkspace(data) {
  return request("POST", "/api/v1/workspaces", { body: data });
}

export async function listWorkspaces() {
  return request("GET", "/api/v1/workspaces");
}

// ---------------------------------------------------------------------------
// Workflow API
// ---------------------------------------------------------------------------

export async function createWorkflow(data, workspace) {
  return request("POST", "/api/v1/workflows", { body: data, params: { workspace } });
}

export async function listWorkflows(workspace) {
  return request("GET", "/api/v1/workflows", { params: { workspace } });
}

export async function getWorkflow(id) {
  return request("GET", `/api/v1/workflows/${encodeURIComponent(id)}`);
}

export async function updateWorkflow(id, data) {
  return request("PUT", `/api/v1/workflows/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteWorkflow(id) {
  await request("DELETE", `/api/v1/workflows/${encodeURIComponent(id)}`);
}

export async function publishWorkflow(id) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(id)}/publish`);
}

export async function validateWorkflow(id) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(id)}/validate`);
}

export async function transitionWorkflow(id, status) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(id)}/transition`, {
    body: { status },
  });
}

// ---------------------------------------------------------------------------
// Node API
// ---------------------------------------------------------------------------

export async function addNode(workflowId, data) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(workflowId)}/nodes`, {
    body: data,
  });
}

export async function getNodes(workflowId) {
  return request("GET", `/api/v1/workflows/${encodeURIComponent(workflowId)}/nodes`);
}

export async function updateNode(workflowId, nodeId, data) {
  return request(
    "PUT",
    `/api/v1/workflows/${encodeURIComponent(workflowId)}/nodes/${encodeURIComponent(nodeId)}`,
    { body: data },
  );
}

export async function deleteNode(workflowId, nodeId) {
  await request(
    "DELETE",
    `/api/v1/workflows/${encodeURIComponent(workflowId)}/nodes/${encodeURIComponent(nodeId)}`,
  );
}

// ---------------------------------------------------------------------------
// Edge API
// ---------------------------------------------------------------------------

export async function addEdge(workflowId, data) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(workflowId)}/edges`, {
    body: data,
  });
}

export async function getEdges(workflowId) {
  return request("GET", `/api/v1/workflows/${encodeURIComponent(workflowId)}/edges`);
}

export async function deleteEdge(workflowId, edgeId) {
  await request(
    "DELETE",
    `/api/v1/workflows/${encodeURIComponent(workflowId)}/edges/${encodeURIComponent(edgeId)}`,
  );
}

// ---------------------------------------------------------------------------
// Execution API
// ---------------------------------------------------------------------------

export async function executeWorkflow(workflowId, inputs) {
  return request("POST", `/api/v1/workflows/${encodeURIComponent(workflowId)}/execute`, {
    body: { inputs },
  });
}

export async function listWorkflowExecutions(workflowId) {
  return request("GET", `/api/v1/workflows/${encodeURIComponent(workflowId)}/executions`);
}

export async function listExecutions() {
  return request("GET", "/api/v1/executions");
}

export async function getExecution(id) {
  return request("GET", `/api/v1/executions/${encodeURIComponent(id)}`);
}

export async function cancelExecution(id) {
  return request("POST", `/api/v1/executions/${encodeURIComponent(id)}/cancel`);
}

export async function retryExecution(id) {
  return request("POST", `/api/v1/executions/${encodeURIComponent(id)}/retry`);
}

// ---------------------------------------------------------------------------
// Trigger API
// ---------------------------------------------------------------------------

export async function createTrigger(data) {
  return request("POST", "/api/v1/triggers", { body: data });
}

export async function listTriggers(workflowId) {
  return request("GET", "/api/v1/triggers", { params: { workflowId } });
}

export async function updateTrigger(id, data) {
  return request("PUT", `/api/v1/triggers/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteTrigger(id) {
  await request("DELETE", `/api/v1/triggers/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// Credential API
// ---------------------------------------------------------------------------

export async function createCredential(data, workspace) {
  return request("POST", "/api/v1/credentials", { body: data, params: { workspace } });
}

export async function listCredentials(workspace) {
  return request("GET", "/api/v1/credentials", { params: { workspace } });
}

export async function updateCredential(id, data) {
  return request("PUT", `/api/v1/credentials/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteCredential(id) {
  await request("DELETE", `/api/v1/credentials/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// Variable API
// ---------------------------------------------------------------------------

export async function createVariable(data, workspace) {
  return request("POST", "/api/v1/variables", { body: data, params: { workspace } });
}

export async function listVariables(workspace) {
  return request("GET", "/api/v1/variables", { params: { workspace } });
}

export async function updateVariable(id, data) {
  return request("PUT", `/api/v1/variables/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteVariable(id) {
  await request("DELETE", `/api/v1/variables/${encodeURIComponent(id)}`);
}
