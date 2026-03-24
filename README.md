# Ground Control

[![CI](https://github.com/KeplerOps/Ground-Control/actions/workflows/ci.yml/badge.svg)](https://github.com/KeplerOps/Ground-Control/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)

A lightweight workflow management platform for building, scheduling, and
monitoring automated workflows. Think Airflow or n8n, but simpler.

Ground Control lets you define workflows as directed acyclic graphs (DAGs),
execute them on a schedule or via triggers, and monitor everything through
your choice of API, GUI, or AI assistant (MCP). Every workflow, node, edge,
and execution is stored in a graph-native data layer — so questions like
"what downstream tasks break if this step fails?" are a single query away.

## Key Features

- **Visual workflow builder** — Drag-and-drop DAG editor in the browser
- **DAG execution engine** — Topological task scheduling with retry, timeout, and parallelism controls
- **Task types** — Shell commands, HTTP requests, and Docker containers out of the box
- **Triggers** — Cron schedules, incoming webhooks, and manual execution
- **Triple interface** — REST API + MCP server for AI assistants + web GUI
- **Graph-native analysis** — Cycle detection, critical path, dependency impact analysis powered by Apache AGE
- **Workspaces** — Organize workflows by team or project
- **Credentials and variables** — Securely store secrets and share configuration across workflows
- **Audit trail** — Every change to every entity is versioned via Hibernate Envers

## Quick Start

**Prerequisites:** Docker and Docker Compose

```bash
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
cp .env.example .env

docker compose up -d    # PostgreSQL 16 (Apache AGE) + Ground Control
```

Then visit:

- **GUI** — `http://localhost:3000`
- **API** — `http://localhost:8000/api/v1/workflows`
- **Swagger UI** — `http://localhost:8000/api/docs`
- **OpenAPI spec** — `http://localhost:8000/api/openapi.json`

### Development Setup

For local development without Docker for the app layer:

```bash
make up       # Start PostgreSQL 16 (Apache AGE)
make dev      # Spring Boot on http://localhost:8000
```

In a separate terminal:

```bash
make frontend-install   # Install frontend dependencies
make frontend-dev       # Vite dev server on http://localhost:3000
```

### MCP Server (Claude Code)

Configured in `.mcp.json`, works automatically with Claude Code. Start the
backend, then use tools like `gc_create_workflow`, `gc_list_executions`, and
`gc_trigger_workflow` from your conversation. See the
[MCP server docs](mcp/ground-control/README.md) for the full tool reference.

## Architecture

```
api/ -> domain/ <- infrastructure/
```

The domain layer has zero Spring web imports. Controllers depend on domain
services; infrastructure adapters implement domain interfaces. Enforced at
compile time by ArchUnit.

```
com.keplerops.groundcontrol/
├── api/               Controllers, DTOs, exception handling
├── domain/            Entities, services, enums, repository interfaces
│   ├── workspaces/    Workspace entity, service
│   ├── workflows/     Workflow, Node, Edge entities, execution engine
│   ├── triggers/      Trigger entity, cron scheduler
│   ├── credentials/   Credential entity, encryption
│   └── variables/     Variable entity, scoping
├── infrastructure/    AGE graph, task executors, webhook listener
└── shared/            Request logging, MDC
```

The workflow domain model centers on **Workflows** (DAGs of Nodes and Edges),
**Executions** (runtime instances of a workflow), and **Triggers** (cron,
webhook, or manual). PostgreSQL stores relational data; Apache AGE provides
graph queries for dependency analysis, critical path computation, and
impact analysis.

## API Endpoints

| Resource | Operations |
|----------|-----------|
| **Workspaces** | CRUD |
| **Workflows** | CRUD, publish, validate, status transitions |
| **Nodes** | CRUD within a workflow |
| **Edges** | CRUD within a workflow |
| **Executions** | Create, list, get, cancel, retry, stats |
| **Triggers** | CRUD, enable/disable |
| **Credentials** | CRUD (values write-only) |
| **Variables** | CRUD |
| **Webhooks** | `POST /api/v1/webhooks/{token}` |

See the [API Reference](docs/API.md) for full details.

## Development

```bash
make rapid        # Format + compile (~3-5s) — inner dev loop
make test         # Unit tests
make check        # CI-equivalent: build + tests + static analysis + coverage
make integration  # Integration tests (Testcontainers, no external DB needed)
make verify       # Everything: check + integration + OpenJML ESC
```

Run `make help` to see all targets.

## Technology Stack

| | |
|---|---|
| **Runtime** | Java 21 / Spring Boot 3.4 / Gradle |
| **Frontend** | React 19 / Vite 6 / TypeScript 5 / Tailwind 4 |
| **Database** | PostgreSQL 16 + Apache AGE (graph queries) |
| **Migrations** | Flyway |
| **Auditing** | Hibernate Envers |
| **Testing** | JUnit 5, jqwik (property-based), ArchUnit, Testcontainers |
| **Static analysis** | Spotless, Error Prone, SpotBugs, Checkstyle, JaCoCo |
| **Formal methods** | JML + OpenJML ESC + Z3 |
| **CI/CD** | GitHub Actions -> GHCR |
| **Quality** | SonarCloud |

## Documentation

| Document | Description |
|----------|-------------|
| [API Reference](docs/API.md) | REST endpoints, filtering, pagination, error format |
| [Architecture](docs/architecture/ARCHITECTURE.md) | Domain model, execution engine, package structure |
| [Coding Standards](docs/CODING_STANDARDS.md) | Style, testing policy, assurance levels |
| [Deployment](docs/deployment/DEPLOYMENT.md) | Setup, Docker, CI/CD pipeline |
| [MCP Server](mcp/ground-control/README.md) | Tool reference, workflows |
| [ADRs](architecture/adrs/) | Architecture Decision Records |
| [Contributing](CONTRIBUTING.md) | Setup, workflow, PR process |
| [Changelog](CHANGELOG.md) | Release history |

## License

[MIT](LICENSE)
