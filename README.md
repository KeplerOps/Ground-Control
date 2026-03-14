# Ground Control

[![CI](https://github.com/KeplerOps/Ground-Control/actions/workflows/ci.yml/badge.svg)](https://github.com/KeplerOps/Ground-Control/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)

Requirements that stay connected to the work that implements them.

Ground Control tracks requirements, links them to code, tests, issues, and
specs, then tells you what's missing, what's circular, and what breaks if
something changes.

## Features

- **Requirements lifecycle** — DRAFT → ACTIVE → DEPRECATED → ARCHIVED, with MoSCoW priority and wave-based planning
- **Traceability links** — Connect requirements to GitHub issues, code files, tests, ADRs, and other artifacts
- **Graph analysis** — Cycle detection, orphan detection, coverage gaps, transitive impact analysis, cross-wave validation
- **GitHub integration** — Sync issues into the traceability graph, or create issues from requirements with one command
- **StrictDoc import** — Bulk-import from `.sdoc` files, idempotent
- **MCP server** — 19 tools for Claude Code: manage requirements, run analysis, and build traceability without leaving your editor
- **Audit trail** — Every change to every entity is versioned

## Getting Started

**Prerequisites:** Java 21, Docker, `gh` CLI (for GitHub features)

```bash
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
cp .env.example .env

make up       # Start PostgreSQL 16 (Apache AGE)
make dev      # Spring Boot on http://localhost:8000
```

Then visit:

- **API** — `http://localhost:8000/api/v1/requirements`
- **Swagger UI** — `http://localhost:8000/api/docs`
- **OpenAPI spec** — `http://localhost:8000/api/openapi.json`

### MCP Server (Claude Code)

Configured in `.mcp.json`, works automatically with Claude Code. Start the
backend, then use tools like `gc_create_requirement`, `gc_analyze_cycles`, and
`gc_create_github_issue` from your conversation. See the
[MCP server docs](mcp/ground-control/README.md) for the full tool reference.

## Development

```bash
make rapid        # Format + compile (~1s warm) — inner dev loop
make test         # Unit tests
make check        # CI-equivalent: build + tests + static analysis + coverage
make integration  # Integration tests (Testcontainers, no external DB needed)
make verify       # Everything: check + integration + OpenJML ESC
```

Run `make help` to see all targets.

## Tech Stack

| | |
|---|---|
| **Runtime** | Java 21 / Spring Boot 3.4 / Gradle |
| **Database** | PostgreSQL 16 + Apache AGE (optional graph queries) |
| **Migrations** | Flyway |
| **Auditing** | Hibernate Envers |
| **Testing** | JUnit 5, jqwik (property-based), ArchUnit, Testcontainers |
| **Static analysis** | Spotless, Error Prone, SpotBugs, Checkstyle, JaCoCo |
| **Formal methods** | JML + OpenJML ESC + Z3 |
| **CI/CD** | GitHub Actions → GHCR |
| **Quality** | SonarCloud |

## Architecture

```
api/ → domain/ ← infrastructure/
```

The domain layer has zero Spring web imports. Controllers depend on domain
services; infrastructure adapters implement domain interfaces. Enforced at
compile time by ArchUnit.

```
com.keplerops.groundcontrol/
├── api/               Controllers, DTOs, exception handling
├── domain/            Entities, services, enums, repository interfaces
├── infrastructure/    AGE graph adapter, GitHub CLI adapter
└── shared/            Request logging, MDC
```

## Documentation

| Document | Description |
|----------|-------------|
| [API Reference](docs/API.md) | REST endpoints, filtering, pagination, error format |
| [Architecture](docs/architecture/ARCHITECTURE.md) | Package structure, dependency rules |
| [Coding Standards](docs/CODING_STANDARDS.md) | Style, testing policy, assurance levels |
| [Deployment](docs/deployment/DEPLOYMENT.md) | Setup, Docker, CI/CD pipeline |
| [MCP Server](mcp/ground-control/README.md) | Tool reference, workflows |
| [ADRs](architecture/adrs/) | Architecture Decision Records |
| [Contributing](CONTRIBUTING.md) | Setup, workflow, PR process |
| [Changelog](CHANGELOG.md) | Release history |

## License

[MIT](LICENSE)
