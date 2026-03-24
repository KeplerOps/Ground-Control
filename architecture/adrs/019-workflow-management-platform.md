# ADR-019: Workflow Management Platform Pivot

## Status

Accepted

## Date

2026-03-24

## Context

Ground Control was originally built as a graph-native requirements management platform with
traceability, formal verification, and analysis capabilities. The core architecture — clean
layered design (api → domain ← infrastructure), Apache AGE graph database, PostgreSQL with
Flyway migrations, Hibernate Envers auditing, and ArchUnit-enforced dependency rules — provides
a solid foundation for any graph-centric domain.

The market for workflow management platforms (Airflow, n8n, Prefect, Rundeck, Temporal) has
well-known pain points:

- **Airflow**: Heavy setup, DAGs-as-code only (no visual editor), slow scheduler, complex
  dependency management, poor local dev experience
- **n8n**: Limited scalability, weak error handling at scale, JavaScript-only extensions
- **Rundeck**: Dated UI, limited workflow complexity, weak API-first design
- **Temporal**: Excellent engine but high operational complexity, steep learning curve
- **All**: Over-engineered enterprise RBAC, bloated dependencies, poor API/MCP integration

Ground Control's graph core (Apache AGE), clean architecture, and existing infrastructure
(Docker deployment, CI/CD, formal methods pipeline) can be repurposed to build a workflow
management platform that addresses these complaints while maintaining the architectural rigor
that differentiates it.

## Decision

Pivot Ground Control from requirements management to a **workflow management platform** with
the following core capabilities:

### Domain Model

| Entity | Purpose |
|--------|---------|
| **Workspace** | Organizational unit (replaces Project) |
| **Workflow** | DAG definition with lifecycle (DRAFT → ACTIVE → PAUSED → ARCHIVED) |
| **WorkflowNode** | A task/step in a workflow (shell, HTTP, Docker, script, conditional, etc.) |
| **WorkflowEdge** | Directed connection between nodes (data/control flow) |
| **Execution** | A workflow run instance with full lifecycle tracking |
| **TaskExecution** | Individual node execution within a run |
| **Trigger** | What starts a workflow (manual, cron, webhook, event) |
| **Credential** | Encrypted secrets for integrations |
| **Variable** | Workspace/workflow-scoped configuration |

### Execution Engine

- **Task executors**: Shell, HTTP, Docker, Script (Python/Node), Sub-workflow
- **Control flow**: Conditional branching, parallel execution, delays
- **Reliability**: Configurable retry policies with exponential backoff, timeouts
- **Logging**: Per-task execution logs with streaming support

### Triple Interface

- **REST API**: Full CRUD + execution + monitoring (API-first design)
- **MCP Server**: Claude Code integration for workflow management via natural language
- **Web GUI**: Visual workflow builder with DAG editor, execution monitoring, log viewer

### Deployment

- Local Docker Compose as primary deployment target
- Single-JAR with embedded frontend (existing pattern)
- PostgreSQL + Apache AGE (existing infrastructure)
- Docker socket access for container-based task execution

### Design Principles (addressing common complaints)

1. **Simple by default**: No enterprise RBAC, no complex multi-tenancy — just workspaces
2. **Visual + Code**: GUI workflow builder AND API/MCP-driven workflow definition
3. **Fast local dev**: `docker compose up` and you're running workflows
4. **Lightweight**: Single JVM process, no separate scheduler/worker/webserver split
5. **Graph-native**: Workflow DAGs stored in and queryable via Apache AGE
6. **API-first**: Every operation available via REST, then MCP, then GUI

### What's Preserved

- Clean architecture (api → domain ← infrastructure) with ArchUnit enforcement
- PostgreSQL + Apache AGE graph database
- Flyway migrations, Hibernate JPA
- Spring Boot 3.4 / Java 21 platform
- React 19 / Vite 6 / TypeScript frontend stack
- Docker deployment infrastructure
- Formal methods pipeline (JML for state machines)
- CI/CD and static analysis toolchain

### What's Replaced

- Requirements domain model → Workflow domain model
- Requirements API → Workflow/Execution/Trigger API
- Requirements UI → Workflow builder + execution monitor UI
- Requirements MCP tools → Workflow MCP tools
- Traceability/baselines/imports → Executions/triggers/credentials

## Consequences

### Positive

- Leverages existing architectural investment in a higher-demand domain
- Graph core is natural fit for workflow DAGs (better than for requirements)
- Addresses real pain points in existing workflow tools
- API + MCP + GUI triple interface is unique differentiator
- Simple Docker deployment eliminates common "too complex to set up" complaint
- Execution engine can leverage Docker for isolated task execution

### Negative

- All existing requirements-domain code, tests, and MCP tools must be replaced
- Existing users of the requirements platform lose their tool
- Execution engine is complex — reliability and fault tolerance are hard problems
- Docker-in-Docker/Docker socket access has security implications

### Risks

- Workflow execution reliability is table-stakes — must be solid from day one
- Performance under concurrent workflow execution needs careful design
- Secret management (credentials) requires proper encryption from the start
- Docker executor requires careful sandboxing to prevent container escape
