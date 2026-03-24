# Ground Control REST API

HTTP API for the workflow management platform.

## Base URL

```
http://localhost:8000/api/v1/
```

## Endpoints

### Workspaces

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workspaces` | WorkspaceRequest | 201 | Create workspace |
| GET | `/workspaces` | — | 200 | List workspaces (paginated) |
| GET | `/workspaces/{id}` | — | 200 | Get workspace by ID |
| PUT | `/workspaces/{id}` | UpdateWorkspaceRequest | 200 | Update workspace |
| DELETE | `/workspaces/{id}` | — | 204 | Delete workspace |

**WorkspaceRequest:**

```json
{
  "name": "Data Engineering",
  "description": "ETL and data pipeline workflows"
}
```

### Workflows

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workspaces/{workspaceId}/workflows` | WorkflowRequest | 201 | Create workflow |
| GET | `/workspaces/{workspaceId}/workflows` | — | 200 | List workflows (paginated, filterable) |
| GET | `/workflows/{id}` | — | 200 | Get workflow by ID |
| PUT | `/workflows/{id}` | UpdateWorkflowRequest | 200 | Update workflow |
| DELETE | `/workflows/{id}` | — | 204 | Delete workflow |
| POST | `/workflows/{id}/publish` | — | 200 | Publish workflow (validates DAG first) |
| POST | `/workflows/{id}/validate` | — | 200 | Validate workflow DAG without publishing |
| POST | `/workflows/{id}/transition` | `{ "status": "ACTIVE" }` | 200 | Transition workflow status |

**WorkflowRequest:**

```json
{
  "name": "Daily ETL Pipeline",
  "description": "Extract, transform, and load data from source systems",
  "tags": ["etl", "daily"]
}
```

**Workflow statuses:** DRAFT -> ACTIVE -> PAUSED -> ARCHIVED

Publishing a workflow validates the DAG (checks for cycles, disconnected nodes,
and missing configurations) and transitions it from DRAFT to ACTIVE.

### Nodes

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workflows/{workflowId}/nodes` | NodeRequest | 201 | Create node |
| GET | `/workflows/{workflowId}/nodes` | — | 200 | List nodes in workflow |
| GET | `/workflows/{workflowId}/nodes/{id}` | — | 200 | Get node by ID |
| PUT | `/workflows/{workflowId}/nodes/{id}` | UpdateNodeRequest | 200 | Update node |
| DELETE | `/workflows/{workflowId}/nodes/{id}` | — | 204 | Delete node |

**NodeRequest:**

```json
{
  "name": "Extract Orders",
  "type": "HTTP",
  "config": {
    "method": "GET",
    "url": "https://api.example.com/orders",
    "headers": { "Authorization": "Bearer {{credentials.api_key}}" },
    "timeout": 30
  },
  "retryPolicy": {
    "maxRetries": 3,
    "backoffSeconds": 10
  },
  "position": { "x": 100, "y": 200 }
}
```

**Node types:**

| Type | Description | Config fields |
|------|-------------|---------------|
| `SHELL` | Execute a shell command | `command`, `workingDir`, `env`, `timeout` |
| `HTTP` | Make an HTTP request | `method`, `url`, `headers`, `body`, `timeout` |
| `DOCKER` | Run a Docker container | `image`, `command`, `env`, `volumes`, `timeout` |

### Edges

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workflows/{workflowId}/edges` | EdgeRequest | 201 | Create edge |
| GET | `/workflows/{workflowId}/edges` | — | 200 | List edges in workflow |
| GET | `/workflows/{workflowId}/edges/{id}` | — | 200 | Get edge by ID |
| DELETE | `/workflows/{workflowId}/edges/{id}` | — | 204 | Delete edge |

**EdgeRequest:**

```json
{
  "sourceNodeId": "uuid-of-extract-node",
  "targetNodeId": "uuid-of-transform-node",
  "condition": "success"
}
```

**Edge conditions:**

| Condition | Description |
|-----------|-------------|
| `success` | Target runs only if source succeeds (default) |
| `failure` | Target runs only if source fails |
| `always` | Target runs regardless of source outcome |

Creating an edge that would introduce a cycle returns `409 Conflict`.

### Executions

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workflows/{workflowId}/executions` | ExecutionRequest | 201 | Start execution |
| GET | `/workflows/{workflowId}/executions` | — | 200 | List executions (paginated, filterable) |
| GET | `/executions/{id}` | — | 200 | Get execution with node statuses |
| POST | `/executions/{id}/cancel` | — | 200 | Cancel running execution |
| POST | `/executions/{id}/retry` | — | 201 | Retry failed execution |
| GET | `/workflows/{workflowId}/executions/stats` | — | 200 | Execution statistics |

**ExecutionRequest:**

```json
{
  "inputs": {
    "date": "2026-03-24",
    "mode": "full"
  }
}
```

**Execution statuses:** PENDING -> RUNNING -> COMPLETED | FAILED | CANCELLED

**ExecutionResponse:**

```json
{
  "id": "uuid",
  "workflowId": "uuid",
  "status": "RUNNING",
  "triggeredBy": "manual",
  "startedAt": "2026-03-24T10:00:00Z",
  "completedAt": null,
  "inputs": { "date": "2026-03-24" },
  "nodeStatuses": [
    {
      "nodeId": "uuid",
      "nodeName": "Extract Orders",
      "status": "COMPLETED",
      "startedAt": "2026-03-24T10:00:01Z",
      "completedAt": "2026-03-24T10:00:05Z",
      "output": { "recordCount": 1523 },
      "error": null
    },
    {
      "nodeId": "uuid",
      "nodeName": "Transform Data",
      "status": "RUNNING",
      "startedAt": "2026-03-24T10:00:06Z",
      "completedAt": null,
      "output": null,
      "error": null
    }
  ]
}
```

**ExecutionStatsResponse:**

```json
{
  "totalExecutions": 142,
  "completed": 130,
  "failed": 8,
  "cancelled": 4,
  "averageDurationSeconds": 47.2,
  "successRate": 0.916
}
```

### Triggers

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workflows/{workflowId}/triggers` | TriggerRequest | 201 | Create trigger |
| GET | `/workflows/{workflowId}/triggers` | — | 200 | List triggers for workflow |
| GET | `/triggers/{id}` | — | 200 | Get trigger by ID |
| PUT | `/triggers/{id}` | UpdateTriggerRequest | 200 | Update trigger |
| DELETE | `/triggers/{id}` | — | 204 | Delete trigger |
| POST | `/triggers/{id}/toggle` | — | 200 | Enable or disable trigger |

**TriggerRequest (cron):**

```json
{
  "type": "CRON",
  "config": {
    "expression": "0 0 2 * * ?",
    "timezone": "UTC"
  },
  "enabled": true
}
```

**TriggerRequest (webhook):**

```json
{
  "type": "WEBHOOK",
  "config": {
    "secret": "optional-hmac-secret"
  },
  "enabled": true
}
```

**Trigger types:**

| Type | Description |
|------|-------------|
| `CRON` | Runs on a cron schedule |
| `WEBHOOK` | Runs when the webhook URL receives a POST |
| `MANUAL` | Run only via API or GUI |

### Credentials

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workspaces/{workspaceId}/credentials` | CredentialRequest | 201 | Create credential |
| GET | `/workspaces/{workspaceId}/credentials` | — | 200 | List credentials (values redacted) |
| GET | `/credentials/{id}` | — | 200 | Get credential (value redacted) |
| PUT | `/credentials/{id}` | UpdateCredentialRequest | 200 | Update credential |
| DELETE | `/credentials/{id}` | — | 204 | Delete credential |

**CredentialRequest:**

```json
{
  "name": "api_key",
  "type": "SECRET",
  "value": "sk-abc123..."
}
```

Credential values are write-only. GET responses return the name and type but
never the plaintext value. Reference credentials in node configs using the
`{{credentials.name}}` template syntax.

### Variables

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/workspaces/{workspaceId}/variables` | VariableRequest | 201 | Create variable |
| GET | `/workspaces/{workspaceId}/variables` | — | 200 | List variables |
| GET | `/variables/{id}` | — | 200 | Get variable |
| PUT | `/variables/{id}` | UpdateVariableRequest | 200 | Update variable |
| DELETE | `/variables/{id}` | — | 204 | Delete variable |

**VariableRequest:**

```json
{
  "name": "base_url",
  "value": "https://api.example.com",
  "description": "Base URL for the external API"
}
```

Reference variables in node configs using the `{{variables.name}}` template
syntax.

### Webhooks

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/webhooks/{token}` | any JSON | 200 | Trigger workflow via webhook |

The webhook token is generated when a WEBHOOK trigger is created. The request
body is passed to the execution as input. If an HMAC secret is configured on the
trigger, the request must include an `X-Webhook-Signature` header with a valid
HMAC-SHA256 signature of the body.

**Response:**

```json
{
  "executionId": "uuid",
  "status": "PENDING"
}
```

### Analysis

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/workflows/{id}/analysis/critical-path` | — | 200 | Critical path through the DAG |
| GET | `/workflows/{id}/analysis/impact/{nodeId}` | — | 200 | Downstream impact of a node |
| POST | `/workflows/{id}/analysis/validate` | — | 200 | Full DAG validation |

## Request / Response Format

JSON. Error responses use a nested envelope:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Workflow not found",
    "detail": {}
  }
}
```

HTTP status codes: 201 (created), 200 (ok), 204 (deleted), 400 (bad request),
404 (not found), 409 (conflict), 422 (validation error).

## Filtering

`GET /workspaces/{workspaceId}/workflows` accepts query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | enum | DRAFT, ACTIVE, PAUSED, ARCHIVED |
| `search` | string | Free-text search in name and description |
| `tag` | string | Filter by tag (repeatable) |

`GET /workflows/{workflowId}/executions` accepts query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | enum | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| `from` | ISO-8601 instant | Start of date range |
| `to` | ISO-8601 instant | End of date range |

## Pagination

Standard Spring Page parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (0-based) |
| `size` | 20 | Page size |
| `sort` | — | Sort field and direction (e.g. `sort=createdAt,desc`) |

Response wraps results in a Spring Page object with `content`, `totalElements`,
`totalPages`, `number`, `size`.

## Interactive Docs

- Swagger UI: `http://localhost:8000/api/docs`
- OpenAPI JSON: `http://localhost:8000/api/openapi.json`
