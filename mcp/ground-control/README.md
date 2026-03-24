# Ground Control MCP Server

MCP server wrapping the Ground Control REST API. Provides tools for workflow
management including workspaces, workflows, nodes, edges, executions, triggers,
credentials, and variables.

## Setup

Add to your Claude Code MCP config (`.claude/settings.json` or project
`.mcp.json`):

```json
{
  "mcpServers": {
    "ground-control": {
      "command": "node",
      "args": ["/path/to/Ground-Control/mcp/ground-control/index.js"],
      "env": {
        "GC_BASE_URL": "http://localhost:8000"
      }
    }
  }
}
```

Requires a running Ground Control instance:

```sh
make up && make dev
```

`GC_BASE_URL` defaults to `http://localhost:8000` if not set. The API base
path is `/api/v1`.

## Workflow

Operations have a natural ordering. Workspaces contain workflows, workflows
contain nodes and edges, and executions are created from published workflows.

1. **Create a workspace** -- `gc_create_workspace` to organize workflows
2. **Create a workflow** -- `gc_create_workflow` with a name (defaults to DRAFT)
3. **Add nodes** -- `gc_add_node` to define steps (actions, conditions, transforms, etc.)
4. **Add edges** -- `gc_add_edge` to connect nodes with optional conditions
5. **Validate** -- `gc_validate_workflow` to check structure before publishing
6. **Publish** -- `gc_publish_workflow` to make the workflow executable
7. **Execute** -- `gc_execute_workflow` to run with optional inputs
8. **Monitor** -- `gc_list_executions`, `gc_get_execution` to track progress
9. **Manage lifecycle** -- `gc_transition_workflow` to ACTIVE, PAUSED, or ARCHIVED
10. **Set up triggers** -- `gc_create_trigger` for webhooks, schedules, or events
11. **Configure credentials** -- `gc_create_credential` for external service auth
12. **Define variables** -- `gc_create_variable` for reusable workspace-level values

## Tool Reference

### Workspaces

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_create_workspace` | `name` (required), `description` | Create a workspace for organizing workflows |
| `gc_list_workspaces` | _(none)_ | List all workspaces |

### Workflows

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_create_workflow` | `name` (required), `description`, `workspace` | Create a workflow. Status defaults to DRAFT |
| `gc_list_workflows` | `workspace` | List workflows, optionally filtered by workspace |
| `gc_get_workflow` | `id` (required) | Get a workflow by UUID |
| `gc_update_workflow` | `id` (required), `name`, `description` | Update workflow fields. Pass only changed fields |
| `gc_delete_workflow` | `id` (required) | Delete a workflow |
| `gc_publish_workflow` | `id` (required) | Publish a workflow for execution |
| `gc_validate_workflow` | `id` (required) | Validate workflow structure (nodes, edges, config) |
| `gc_transition_workflow` | `id` (required), `status` (required) | Transition workflow status |

### Nodes

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_add_node` | `workflow_id` (required), `name` (required), `node_type` (required), `config`, `position_x`, `position_y` | Add a node to a workflow |
| `gc_get_nodes` | `workflow_id` (required) | Get all nodes in a workflow |
| `gc_update_node` | `workflow_id` (required), `node_id` (required), `name`, `config`, `position_x`, `position_y` | Update a node. Pass only changed fields |
| `gc_delete_node` | `workflow_id` (required), `node_id` (required) | Delete a node from a workflow |

### Edges

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_add_edge` | `workflow_id` (required), `source_node_id` (required), `target_node_id` (required), `condition` | Add an edge between two nodes |
| `gc_get_edges` | `workflow_id` (required) | Get all edges in a workflow |
| `gc_delete_edge` | `workflow_id` (required), `edge_id` (required) | Delete an edge from a workflow |

### Executions

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_execute_workflow` | `workflow_id` (required), `inputs` | Execute a published workflow with optional JSON inputs |
| `gc_list_executions` | `workflow_id` | List executions. Filters by workflow if ID provided, otherwise lists all |
| `gc_get_execution` | `id` (required) | Get execution details |
| `gc_cancel_execution` | `id` (required) | Cancel a running execution |
| `gc_retry_execution` | `id` (required) | Retry a failed execution |

### Triggers

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_create_trigger` | `workflow_id` (required), `name` (required), `trigger_type` (required), `config` | Create a trigger for a workflow |
| `gc_list_triggers` | `workflow_id` (required) | List triggers for a workflow |
| `gc_toggle_trigger` | `id` (required), `is_active` (required) | Enable or disable a trigger |
| `gc_delete_trigger` | `id` (required) | Delete a trigger |

### Credentials

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_list_credentials` | `workspace` | List credentials in a workspace. Values are never returned |
| `gc_create_credential` | `name` (required), `credential_type` (required), `encrypted_data` (required), `workspace` | Create a credential for workflow nodes |
| `gc_delete_credential` | `id` (required) | Delete a credential |

### Variables

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `gc_list_variables` | `workspace` | List variables in a workspace |
| `gc_create_variable` | `name` (required), `value` (required), `variable_type`, `is_secret`, `workspace` | Create a workspace variable |
| `gc_update_variable` | `id` (required), `name`, `value`, `variable_type`, `is_secret` | Update a variable. Pass only changed fields |
| `gc_delete_variable` | `id` (required) | Delete a variable |

## Enums

**Workflow status:** `DRAFT`, `ACTIVE`, `PAUSED`, `ARCHIVED`

**Node type:** `TRIGGER`, `ACTION`, `CONDITION`, `DELAY`, `LOOP`, `TRANSFORM`, `HTTP_REQUEST`, `CODE`, `SUB_WORKFLOW`

**Trigger type:** `WEBHOOK`, `SCHEDULE`, `EVENT`, `MANUAL`

## Status Transitions

Workflows start in DRAFT. After publishing, they can be transitioned:

```
DRAFT -> (publish) -> PUBLISHED
PUBLISHED -> ACTIVE -> PAUSED -> ARCHIVED
                  \-> ARCHIVED
```

`gc_transition_workflow` accepts `ACTIVE`, `PAUSED`, or `ARCHIVED` as the
target status.

## Error Handling

Errors return:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Workflow not found",
    "detail": {}
  }
}
```

Common codes: `NOT_FOUND` (404), `CONFLICT` (409), `VALIDATION_ERROR` (422).

## IDs

All entities use UUID identifiers, returned in create/list responses. The MCP
tools use `snake_case` parameter names which are automatically converted to
`camelCase` for the REST API.

## Field Name Convention

The MCP server uses `snake_case` field names (e.g., `node_type`, `workflow_id`).
These are automatically mapped to the `camelCase` names expected by the REST API
(e.g., `nodeType`, `workflowId`). Response bodies are similarly converted back
to `snake_case`.
