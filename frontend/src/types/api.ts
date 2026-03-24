export interface Workspace {
  id: string;
  identifier: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface Workflow {
  id: string;
  workspaceId: string;
  name: string;
  description: string;
  status: WorkflowStatus;
  currentVersion: number;
  tags: string;
  timeoutSeconds: number;
  maxRetries: number;
  retryBackoffMs: number;
  createdAt: string;
  updatedAt: string;
}

export type WorkflowStatus = "DRAFT" | "ACTIVE" | "PAUSED" | "ARCHIVED";

export interface WorkflowNode {
  id: string;
  workflowId: string;
  name: string;
  label: string;
  nodeType: NodeType;
  config: string;
  positionX: number;
  positionY: number;
  timeoutSeconds: number | null;
  retryPolicy: string;
  createdAt: string;
  updatedAt: string;
}

export type NodeType =
  | "SHELL"
  | "HTTP"
  | "DOCKER"
  | "SCRIPT"
  | "CONDITIONAL"
  | "DELAY"
  | "SUB_WORKFLOW"
  | "TRANSFORM"
  | "NOTIFICATION"
  | "NOOP";

export interface WorkflowEdge {
  id: string;
  workflowId: string;
  sourceNodeId: string;
  sourceNodeName: string;
  targetNodeId: string;
  targetNodeName: string;
  conditionExpr: string;
  label: string;
  createdAt: string;
}

export interface Execution {
  id: string;
  workflowId: string;
  workflowName: string;
  workflowVersion: number;
  status: ExecutionStatus;
  triggerType: string;
  triggerRef: string;
  inputs: string;
  outputs: string;
  error: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number;
  createdAt: string;
}

export type ExecutionStatus =
  | "PENDING"
  | "QUEUED"
  | "RUNNING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "SKIPPED"
  | "TIMED_OUT";

export interface TaskExecution {
  id: string;
  executionId: string;
  nodeId: string | null;
  nodeName: string;
  nodeType: NodeType;
  status: ExecutionStatus;
  attempt: number;
  inputs: string;
  outputs: string;
  logs: string;
  error: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number;
  createdAt: string;
}

export interface ExecutionDetail extends Execution {
  tasks: TaskExecution[];
}

export interface Trigger {
  id: string;
  workflowId: string;
  workflowName: string;
  name: string;
  triggerType: "MANUAL" | "CRON" | "WEBHOOK" | "EVENT";
  config: string;
  enabled: boolean;
  lastFiredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Credential {
  id: string;
  workspaceId: string;
  name: string;
  credentialType: string;
  createdAt: string;
  updatedAt: string;
}

export interface Variable {
  id: string;
  workspaceId: string;
  key: string;
  value: string;
  description: string;
  secret: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ExecutionStats {
  total: number;
  success: number;
  failed: number;
  running: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ValidationResult {
  valid: boolean;
  message: string;
}
