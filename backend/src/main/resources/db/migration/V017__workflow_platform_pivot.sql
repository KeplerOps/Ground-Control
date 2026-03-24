-- V017: Pivot from requirements management to workflow management platform (ADR-019)
-- This migration drops all requirements-domain tables and creates the workflow domain schema.

-- Drop old audit tables first (foreign key dependencies)
DROP TABLE IF EXISTS traceability_link_audit CASCADE;
DROP TABLE IF EXISTS requirement_relation_audit CASCADE;
DROP TABLE IF EXISTS requirement_audit CASCADE;

-- Drop old domain tables
DROP TABLE IF EXISTS requirement_embedding CASCADE;
DROP TABLE IF EXISTS github_issue_sync CASCADE;
DROP TABLE IF EXISTS requirement_import CASCADE;
DROP TABLE IF EXISTS traceability_link CASCADE;
DROP TABLE IF EXISTS baseline CASCADE;
DROP TABLE IF EXISTS requirement_relation CASCADE;
DROP TABLE IF EXISTS requirement CASCADE;

-- Rename project to workspace
ALTER TABLE project RENAME TO workspace;

-- Drop AGE graph if it exists (will be recreated for workflows)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'age') THEN
        PERFORM ag_catalog.drop_graph('requirements', true);
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- AGE not installed or graph doesn't exist, safe to continue
    NULL;
END $$;

-- ============================================================
-- Workflow: DAG definition with lifecycle management
-- ============================================================
CREATE TABLE workflow (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspace(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_version INTEGER NOT NULL DEFAULT 0,
    tags            TEXT DEFAULT '',
    timeout_seconds INTEGER DEFAULT 3600,
    max_retries     INTEGER DEFAULT 0,
    retry_backoff_ms INTEGER DEFAULT 1000,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workflow_workspace ON workflow(workspace_id);
CREATE INDEX idx_workflow_status ON workflow(status);

-- ============================================================
-- WorkflowNode: A task/step within a workflow DAG
-- ============================================================
CREATE TABLE workflow_node (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    label           VARCHAR(255) DEFAULT '',
    node_type       VARCHAR(30) NOT NULL,
    config          TEXT DEFAULT '{}',
    position_x      INTEGER DEFAULT 0,
    position_y      INTEGER DEFAULT 0,
    timeout_seconds INTEGER,
    retry_policy    TEXT DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(workflow_id, name)
);

CREATE INDEX idx_workflow_node_workflow ON workflow_node(workflow_id);

-- ============================================================
-- WorkflowEdge: Directed connection between nodes
-- ============================================================
CREATE TABLE workflow_edge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    source_node_id  UUID NOT NULL REFERENCES workflow_node(id) ON DELETE CASCADE,
    target_node_id  UUID NOT NULL REFERENCES workflow_node(id) ON DELETE CASCADE,
    condition_expr  TEXT DEFAULT '',
    label           VARCHAR(255) DEFAULT '',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(workflow_id, source_node_id, target_node_id)
);

CREATE INDEX idx_workflow_edge_workflow ON workflow_edge(workflow_id);
CREATE INDEX idx_workflow_edge_source ON workflow_edge(source_node_id);
CREATE INDEX idx_workflow_edge_target ON workflow_edge(target_node_id);

-- ============================================================
-- Execution: A workflow run instance
-- ============================================================
CREATE TABLE execution (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id      UUID NOT NULL REFERENCES workflow(id),
    workflow_version INTEGER NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    trigger_type     VARCHAR(50) DEFAULT 'MANUAL',
    trigger_ref      VARCHAR(255) DEFAULT '',
    inputs           TEXT DEFAULT '{}',
    outputs          TEXT DEFAULT '{}',
    error            TEXT DEFAULT '',
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    duration_ms      BIGINT DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_execution_workflow ON execution(workflow_id);
CREATE INDEX idx_execution_status ON execution(status);
CREATE INDEX idx_execution_created ON execution(created_at DESC);

-- ============================================================
-- TaskExecution: Individual node execution within a run
-- ============================================================
CREATE TABLE task_execution (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id    UUID NOT NULL REFERENCES execution(id) ON DELETE CASCADE,
    node_id         UUID REFERENCES workflow_node(id) ON DELETE SET NULL,
    node_name       VARCHAR(255) NOT NULL,
    node_type       VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt         INTEGER NOT NULL DEFAULT 1,
    inputs          TEXT DEFAULT '{}',
    outputs         TEXT DEFAULT '{}',
    logs            TEXT DEFAULT '',
    error           TEXT DEFAULT '',
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    duration_ms     BIGINT DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_execution_execution ON task_execution(execution_id);
CREATE INDEX idx_task_execution_status ON task_execution(status);

-- ============================================================
-- Trigger: What starts a workflow
-- ============================================================
CREATE TABLE trigger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    trigger_type    VARCHAR(20) NOT NULL,
    config          TEXT DEFAULT '{}',
    enabled         BOOLEAN NOT NULL DEFAULT true,
    last_fired_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trigger_workflow ON trigger(workflow_id);
CREATE INDEX idx_trigger_type ON trigger(trigger_type);
CREATE INDEX idx_trigger_enabled ON trigger(enabled) WHERE enabled = true;

-- ============================================================
-- Credential: Encrypted secrets for integrations
-- ============================================================
CREATE TABLE credential (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspace(id),
    name            VARCHAR(255) NOT NULL,
    credential_type VARCHAR(50) NOT NULL,
    encrypted_data  TEXT NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(workspace_id, name)
);

CREATE INDEX idx_credential_workspace ON credential(workspace_id);

-- ============================================================
-- Variable: Workspace-scoped configuration
-- ============================================================
CREATE TABLE variable (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspace(id),
    key             VARCHAR(255) NOT NULL,
    value           TEXT DEFAULT '',
    description     TEXT DEFAULT '',
    secret          BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(workspace_id, key)
);

CREATE INDEX idx_variable_workspace ON variable(workspace_id);

-- ============================================================
-- Recreate AGE graph for workflows (if AGE extension exists)
-- ============================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'age') THEN
        PERFORM ag_catalog.create_graph('workflows');
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;
