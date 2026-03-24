#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  createWorkspace,
  listWorkspaces,
  createWorkflow,
  listWorkflows,
  getWorkflow,
  updateWorkflow,
  deleteWorkflow,
  publishWorkflow,
  validateWorkflow,
  transitionWorkflow,
  addNode,
  getNodes,
  updateNode,
  deleteNode,
  addEdge,
  getEdges,
  deleteEdge,
  executeWorkflow,
  listWorkflowExecutions,
  listExecutions,
  getExecution,
  cancelExecution,
  retryExecution,
  createTrigger,
  listTriggers,
  updateTrigger,
  deleteTrigger,
  createCredential,
  listCredentials,
  deleteCredential,
  createVariable,
  listVariables,
  updateVariable,
  deleteVariable,
  WORKFLOW_STATUSES,
  NODE_TYPES,
  TRIGGER_TYPES,
} from "./lib.js";

function ok(text) {
  return { content: [{ type: "text", text }] };
}

function err(e) {
  return { content: [{ type: "text", text: `Error: ${e.message}` }], isError: true };
}

const server = new McpServer({ name: "ground-control", version: "2.0.0" });

// ==========================================================================
// Workspace tools
// ==========================================================================

server.tool(
  "gc_create_workspace",
  "Create a new workspace for organizing workflows.",
  {
    name: z.string().describe("Workspace name"),
    description: z.string().optional().describe("Workspace description"),
  },
  async ({ name, description }) => {
    try {
      const data = { name };
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await createWorkspace(data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_workspaces",
  "List all workspaces.",
  {},
  async () => {
    try {
      return ok(JSON.stringify(await listWorkspaces(), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Workflow tools
// ==========================================================================

server.tool(
  "gc_create_workflow",
  "Create a new workflow. Status defaults to DRAFT.",
  {
    name: z.string().describe("Workflow name"),
    description: z.string().optional().describe("Workflow description"),
    workspace: z.string().optional().describe("Workspace ID to create the workflow in"),
  },
  async ({ name, description, workspace }) => {
    try {
      const data = { name };
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await createWorkflow(data, workspace), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_workflows",
  "List workflows, optionally filtered by workspace.",
  {
    workspace: z.string().optional().describe("Workspace ID to filter by"),
  },
  async ({ workspace }) => {
    try {
      return ok(JSON.stringify(await listWorkflows(workspace), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_workflow",
  "Get a workflow by ID.",
  {
    id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getWorkflow(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_workflow",
  "Update a workflow's fields. Pass only the fields to change.",
  {
    id: z.string().uuid().describe("Workflow UUID"),
    name: z.string().optional().describe("New name"),
    description: z.string().optional().describe("New description"),
  },
  async ({ id, name, description }) => {
    try {
      const data = {};
      if (name !== undefined) data.name = name;
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await updateWorkflow(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_workflow",
  "Delete a workflow by ID.",
  {
    id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ id }) => {
    try {
      await deleteWorkflow(id);
      return ok("Workflow deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_publish_workflow",
  "Publish a workflow, making it available for execution.",
  {
    id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await publishWorkflow(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_validate_workflow",
  "Validate a workflow's structure (nodes, edges, configuration).",
  {
    id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await validateWorkflow(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_workflow",
  "Transition a workflow's status. Valid statuses: ACTIVE, PAUSED, ARCHIVED.",
  {
    id: z.string().uuid().describe("Workflow UUID"),
    status: z.enum(WORKFLOW_STATUSES).describe("Target status"),
  },
  async ({ id, status }) => {
    try {
      return ok(JSON.stringify(await transitionWorkflow(id, status), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Node tools
// ==========================================================================

server.tool(
  "gc_add_node",
  "Add a node to a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    name: z.string().describe("Node name"),
    node_type: z.enum(NODE_TYPES).describe("Node type"),
    config: z.string().optional().describe("Node configuration as JSON string"),
    position_x: z.number().optional().describe("X position on canvas"),
    position_y: z.number().optional().describe("Y position on canvas"),
  },
  async ({ workflow_id, name, node_type, config, position_x, position_y }) => {
    try {
      const data = { name, node_type };
      if (config !== undefined) data.config = config;
      if (position_x !== undefined) data.position_x = position_x;
      if (position_y !== undefined) data.position_y = position_y;
      return ok(JSON.stringify(await addNode(workflow_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_nodes",
  "Get all nodes in a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ workflow_id }) => {
    try {
      const nodes = await getNodes(workflow_id);
      if (Array.isArray(nodes) && nodes.length === 0) return ok("No nodes found.");
      return ok(JSON.stringify(nodes, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_node",
  "Update a node in a workflow. Pass only the fields to change.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    node_id: z.string().uuid().describe("Node UUID"),
    name: z.string().optional().describe("New name"),
    config: z.string().optional().describe("New configuration as JSON string"),
    position_x: z.number().optional().describe("New X position"),
    position_y: z.number().optional().describe("New Y position"),
  },
  async ({ workflow_id, node_id, name, config, position_x, position_y }) => {
    try {
      const data = {};
      if (name !== undefined) data.name = name;
      if (config !== undefined) data.config = config;
      if (position_x !== undefined) data.position_x = position_x;
      if (position_y !== undefined) data.position_y = position_y;
      return ok(JSON.stringify(await updateNode(workflow_id, node_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_node",
  "Delete a node from a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    node_id: z.string().uuid().describe("Node UUID"),
  },
  async ({ workflow_id, node_id }) => {
    try {
      await deleteNode(workflow_id, node_id);
      return ok("Node deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Edge tools
// ==========================================================================

server.tool(
  "gc_add_edge",
  "Add an edge (connection) between two nodes in a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    source_node_id: z.string().uuid().describe("Source node UUID"),
    target_node_id: z.string().uuid().describe("Target node UUID"),
    condition: z.string().optional().describe("Edge condition expression"),
  },
  async ({ workflow_id, source_node_id, target_node_id, condition }) => {
    try {
      const data = { source_node_id, target_node_id };
      if (condition !== undefined) data.condition = condition;
      return ok(JSON.stringify(await addEdge(workflow_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_edges",
  "Get all edges (connections) in a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ workflow_id }) => {
    try {
      const edges = await getEdges(workflow_id);
      if (Array.isArray(edges) && edges.length === 0) return ok("No edges found.");
      return ok(JSON.stringify(edges, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_edge",
  "Delete an edge from a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    edge_id: z.string().uuid().describe("Edge UUID"),
  },
  async ({ workflow_id, edge_id }) => {
    try {
      await deleteEdge(workflow_id, edge_id);
      return ok("Edge deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Execution tools
// ==========================================================================

server.tool(
  "gc_execute_workflow",
  "Execute a published workflow with optional input data.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    inputs: z.string().optional().default("{}").describe("Input data as JSON string"),
  },
  async ({ workflow_id, inputs }) => {
    try {
      return ok(JSON.stringify(await executeWorkflow(workflow_id, inputs), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_executions",
  "List workflow executions. If workflow_id is provided, lists executions for that workflow only. Otherwise lists all executions.",
  {
    workflow_id: z.string().uuid().optional().describe("Workflow UUID (optional, lists all if omitted)"),
  },
  async ({ workflow_id }) => {
    try {
      const result = workflow_id
        ? await listWorkflowExecutions(workflow_id)
        : await listExecutions();
      if (Array.isArray(result) && result.length === 0) return ok("No executions found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_execution",
  "Get details of a specific execution.",
  {
    id: z.string().uuid().describe("Execution UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getExecution(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_cancel_execution",
  "Cancel a running execution.",
  {
    id: z.string().uuid().describe("Execution UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await cancelExecution(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_retry_execution",
  "Retry a failed execution.",
  {
    id: z.string().uuid().describe("Execution UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await retryExecution(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Trigger tools
// ==========================================================================

server.tool(
  "gc_create_trigger",
  "Create a trigger for a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
    name: z.string().describe("Trigger name"),
    trigger_type: z.enum(TRIGGER_TYPES).describe("Trigger type"),
    config: z.string().optional().describe("Trigger configuration as JSON string"),
  },
  async ({ workflow_id, name, trigger_type, config }) => {
    try {
      const data = { workflow_id, name, trigger_type };
      if (config !== undefined) data.config = config;
      return ok(JSON.stringify(await createTrigger(data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_triggers",
  "List triggers for a workflow.",
  {
    workflow_id: z.string().uuid().describe("Workflow UUID"),
  },
  async ({ workflow_id }) => {
    try {
      const triggers = await listTriggers(workflow_id);
      if (Array.isArray(triggers) && triggers.length === 0) return ok("No triggers found.");
      return ok(JSON.stringify(triggers, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_toggle_trigger",
  "Enable or disable a trigger by updating its active state.",
  {
    id: z.string().uuid().describe("Trigger UUID"),
    is_active: z.boolean().describe("Whether the trigger should be active"),
  },
  async ({ id, is_active }) => {
    try {
      return ok(JSON.stringify(await updateTrigger(id, { is_active }), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_trigger",
  "Delete a trigger.",
  {
    id: z.string().uuid().describe("Trigger UUID"),
  },
  async ({ id }) => {
    try {
      await deleteTrigger(id);
      return ok("Trigger deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Credential tools
// ==========================================================================

server.tool(
  "gc_list_credentials",
  "List credentials in a workspace. Credential values are never returned.",
  {
    workspace: z.string().optional().describe("Workspace ID to filter by"),
  },
  async ({ workspace }) => {
    try {
      const creds = await listCredentials(workspace);
      if (Array.isArray(creds) && creds.length === 0) return ok("No credentials found.");
      return ok(JSON.stringify(creds, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_credential",
  "Create a credential for use in workflow nodes.",
  {
    name: z.string().describe("Credential name"),
    credential_type: z.string().describe("Credential type (e.g. 'API_KEY', 'OAUTH2', 'BASIC_AUTH')"),
    encrypted_data: z.string().describe("Credential data as JSON string (will be encrypted at rest)"),
    workspace: z.string().optional().describe("Workspace ID"),
  },
  async ({ name, credential_type, encrypted_data, workspace }) => {
    try {
      const data = { name, credential_type, encrypted_data };
      return ok(JSON.stringify(await createCredential(data, workspace), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_credential",
  "Delete a credential.",
  {
    id: z.string().uuid().describe("Credential UUID"),
  },
  async ({ id }) => {
    try {
      await deleteCredential(id);
      return ok("Credential deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Variable tools
// ==========================================================================

server.tool(
  "gc_list_variables",
  "List variables in a workspace.",
  {
    workspace: z.string().optional().describe("Workspace ID to filter by"),
  },
  async ({ workspace }) => {
    try {
      const vars = await listVariables(workspace);
      if (Array.isArray(vars) && vars.length === 0) return ok("No variables found.");
      return ok(JSON.stringify(vars, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_variable",
  "Create a workspace variable for use in workflows.",
  {
    name: z.string().describe("Variable name"),
    value: z.string().describe("Variable value"),
    variable_type: z.string().optional().describe("Variable type (e.g. 'STRING', 'NUMBER', 'BOOLEAN', 'JSON')"),
    is_secret: z.boolean().optional().describe("Whether the variable value is sensitive"),
    workspace: z.string().optional().describe("Workspace ID"),
  },
  async ({ name, value, variable_type, is_secret, workspace }) => {
    try {
      const data = { name, value };
      if (variable_type !== undefined) data.variable_type = variable_type;
      if (is_secret !== undefined) data.is_secret = is_secret;
      return ok(JSON.stringify(await createVariable(data, workspace), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_variable",
  "Update a variable's value or properties.",
  {
    id: z.string().uuid().describe("Variable UUID"),
    name: z.string().optional().describe("New name"),
    value: z.string().optional().describe("New value"),
    variable_type: z.string().optional().describe("New variable type"),
    is_secret: z.boolean().optional().describe("Whether the variable value is sensitive"),
  },
  async ({ id, name, value, variable_type, is_secret }) => {
    try {
      const data = {};
      if (name !== undefined) data.name = name;
      if (value !== undefined) data.value = value;
      if (variable_type !== undefined) data.variable_type = variable_type;
      if (is_secret !== undefined) data.is_secret = is_secret;
      return ok(JSON.stringify(await updateVariable(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_variable",
  "Delete a variable.",
  {
    id: z.string().uuid().describe("Variable UUID"),
  },
  async ({ id }) => {
    try {
      await deleteVariable(id);
      return ok("Variable deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Start server
// ==========================================================================

const transport = new StdioServerTransport();
await server.connect(transport);
