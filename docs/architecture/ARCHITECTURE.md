# Ground Control — Architecture

## Mission

Ground Control is a lightweight workflow management platform for building,
scheduling, and monitoring automated workflows. It lets users define workflows
as directed acyclic graphs (DAGs) of executable nodes, trigger them via cron
schedules, webhooks, or manual invocation, and monitor execution through a
triple interface: REST API, MCP server (for AI assistants), and web GUI.

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.4 |
| Build | Gradle (Kotlin DSL) with included wrapper |
| Database | PostgreSQL 16 + Apache AGE (graph queries) |
| ORM | Hibernate 6 + Spring Data JPA |
| Auditing | Hibernate Envers |
| Migrations | Flyway |
| Contracts | JML (verified by OpenJML ESC + Z3) |
| Testing | JUnit 5 + jqwik + ArchUnit + Testcontainers |
| Static analysis | Error Prone, SpotBugs, Checkstyle |
| Formatting | Spotless + Palantir Java Format |
| Coverage | JaCoCo |
| Logging | SLF4J + Logback (JSON in prod, console in dev) |
| API docs | Springdoc-OpenAPI |
| Frontend | React 19 / Vite 6 / TypeScript 5 / Tailwind 4 |
| Container | Docker (multi-stage, non-root, JDK 21) |
| Registry | GHCR (`ghcr.io/keplerops/ground-control`) |

## Package Structure

```
backend/src/main/java/com/keplerops/groundcontrol/
├── api/                          # REST controllers, DTOs, exception handler
│   ├── workspaces/               # WorkspaceController, request/response records
│   ├── workflows/                # WorkflowController, NodeController, EdgeController
│   ├── executions/               # ExecutionController
│   ├── triggers/                 # TriggerController
│   ├── credentials/              # CredentialController
│   ├── variables/                # VariableController
│   ├── webhooks/                 # WebhookController
│   ├── analysis/                 # AnalysisController (critical path, impact, validation)
│   └── GlobalExceptionHandler.java
├── domain/                       # Business logic (Spring-web-free)
│   ├── exception/                # Domain exception hierarchy
│   ├── workspaces/               # Workspace entity, repository, service
│   ├── workflows/                # Workflow, Node, Edge entities, services
│   ├── executions/               # Execution entity, ExecutionEngine, task runners
│   ├── triggers/                 # Trigger entity, cron scheduler, webhook dispatch
│   ├── credentials/              # Credential entity, encryption service
│   ├── variables/                # Variable entity, template resolution
│   └── analysis/                 # AnalysisService (cycles, critical path, impact)
├── infrastructure/               # External adapter implementations
│   ├── age/                      # AgeGraphService (Apache AGE Cypher queries)
│   ├── execution/                # ShellTaskExecutor, HttpTaskExecutor, DockerTaskExecutor
│   ├── webhook/                  # Webhook listener, HMAC validation
│   └── web/                      # CORS config, SPA routing
├── shared/
│   └── logging/                  # RequestLoggingFilter (MDC request_id)
└── GroundControlApplication.java
```

## Domain Model

The workflow domain model has three core concepts:

**Workflows** are DAGs composed of Nodes and Edges. A Workflow belongs to a
Workspace and progresses through statuses: DRAFT -> ACTIVE -> PAUSED -> ARCHIVED.
Publishing a workflow validates the DAG (cycle detection, connectivity, config
completeness) and transitions it to ACTIVE.

**Nodes** represent individual tasks within a workflow. Each node has a type
(SHELL, HTTP, or DOCKER), a configuration block, an optional retry policy, and
a position for the visual editor. Nodes connect via Edges, which carry a
condition (success, failure, or always) that controls execution flow.

**Executions** are runtime instances of a workflow. When triggered, the execution
engine performs a topological sort of the DAG, schedules nodes respecting
dependency order and edge conditions, and tracks per-node status
(PENDING -> RUNNING -> COMPLETED | FAILED | CANCELLED). The engine supports
retry with configurable backoff, timeouts, and cancellation.

Supporting entities:

- **Triggers** — Cron, webhook, or manual. Cron triggers use a scheduler;
  webhook triggers generate a unique token and accept POST requests.
- **Credentials** — Encrypted secrets scoped to a workspace, referenced in node
  configs via `{{credentials.name}}` template syntax. Values are write-only.
- **Variables** — Key-value pairs scoped to a workspace, referenced via
  `{{variables.name}}`. Unlike credentials, values are readable.

## Execution Engine

The execution engine is the core runtime component:

1. **Trigger** — A cron tick, webhook POST, or manual API call initiates an
   execution for a published workflow.
2. **Plan** — The engine topologically sorts the workflow DAG to determine
   execution order.
3. **Schedule** — Nodes with all dependencies satisfied are dispatched to the
   appropriate task executor (Shell, HTTP, or Docker).
4. **Execute** — Each task executor runs the node's configuration, captures
   output and errors, and reports status back to the engine.
5. **Evaluate** — After a node completes, the engine evaluates outgoing edge
   conditions to determine which downstream nodes to schedule next.
6. **Complete** — The execution completes when all reachable nodes have finished
   (or the execution is cancelled).

Retry logic is per-node: on failure, the engine respects the node's retry policy
(max retries, backoff interval) before marking it as failed.

## Triple Interface

Ground Control exposes three interfaces to the same domain layer:

- **REST API** — Full CRUD for all entities, execution management, and graph
  analysis endpoints. Documented via Springdoc-OpenAPI.
- **MCP Server** — Tool-based interface for AI assistants (Claude Code). Tools
  like `gc_create_workflow`, `gc_trigger_workflow`, and `gc_list_executions`
  call the same domain services as the REST API.
- **Web GUI** — React frontend with a visual DAG editor, execution monitoring
  dashboard, and workspace management. Communicates with the backend via the
  REST API.

## Dependency Rule

```
api/ -> domain/ <- infrastructure/
```

- `domain/` has no imports from `api/` or `infrastructure/` and no Spring web imports
- `api/` depends on `domain/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`

Enforced at compile time by ArchUnit tests in `ArchitectureTest.java`.

## Data Layer

PostgreSQL 16 stores all relational data (workflows, nodes, edges, executions,
triggers, credentials, variables) via Hibernate/JPA. Apache AGE extends
PostgreSQL with a graph query layer — workflow DAGs are materialized as graph
vertices and edges, enabling efficient Cypher queries for:

- **Cycle detection** — Validates DAG integrity before publishing
- **Critical path** — Identifies the longest execution path through the workflow
- **Impact analysis** — Determines downstream nodes affected by a given node's
  failure
- **Dependency queries** — Answers "what are all ancestors/descendants of this
  node?" in a single query

Apache AGE is optional — the application gracefully degrades to JPA-only
analysis when AGE is unavailable.

## Configuration

Spring profiles drive environment-specific behavior:

- `application.yml` — base config (datasource, JPA, Flyway, server port)
- `application-test.yml` — test overrides (Testcontainers)

Environment variables use the `GC_` prefix (e.g., `GC_DATABASE_URL`, `GC_SERVER_PORT`). See `.env.example`.
