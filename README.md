# Ground Control

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Requirements management system with traceability and graph analysis. Java 21 / Spring Boot 3.4, PostgreSQL + Apache AGE. Pre-alpha.

## What is Ground Control?

Ground Control manages requirements, tracks relations between them, links requirements to external artifacts (GitHub issues, code, specs), and runs graph analysis — cycle detection, orphan detection, coverage gap analysis, impact analysis, and cross-wave validation. It supports StrictDoc import and GitHub issue sync for keeping requirements connected to implementation work. Apache AGE provides optional graph queries over the requirements DAG. OpenJML verifies state machine contracts on domain enums.

## Quick Start

```bash
cp .env.example .env
make up                            # PostgreSQL 16 (Apache AGE)
make rapid                         # Format + compile (~1s with warm daemon)
make test                          # Unit tests
make dev                           # Spring Boot dev server on :8000
```

## Repository Structure

```
Ground-Control/
├── backend/                  # Java 21 / Spring Boot 3.4 (Gradle)
│   ├── src/main/java/        # Application source
│   ├── src/main/resources/   # Config, Flyway migrations
│   └── src/test/java/        # JUnit 5, jqwik, ArchUnit
├── architecture/
│   ├── adrs/                 # Architecture Decision Records
│   └── design/               # Design documentation
├── .github/workflows/        # CI + SonarCloud + Docker
├── docs/                     # Operational documentation
│   └── CODING_STANDARDS.md
├── docker-compose.yml        # Dev services (PostgreSQL + AGE)
└── Makefile                  # Common commands (build, test, format, dev)
```

## Documentation

| Document | Description |
|----------|-------------|
| [Coding Standards](docs/CODING_STANDARDS.md) | Style, dependency rule, testing |
| [Contributing](CONTRIBUTING.md) | Setup, workflow, PR process |
| [ADRs](architecture/adrs/) | Architecture Decision Records |

## Status

Pre-alpha. Phase 1 (requirements management) complete: 5 domain entities, StrictDoc import, GitHub issue sync, graph analysis (cycles, orphans, impact, cross-wave), Apache AGE integration, 10 Flyway migrations, REST API (25+ endpoints), E2E integration tests. See [CHANGELOG.md](CHANGELOG.md).

## License

MIT — see [LICENSE](LICENSE).
