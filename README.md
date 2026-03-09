# Ground Control

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Neurosymbolic constraint infrastructure, dogfooded on itself. Java 21 / Spring Boot 3.4 backend with JML contracts, PostgreSQL + Apache AGE, Redis. Pre-alpha.

## Quick Start

```bash
cp .env.example .env
make up                            # PostgreSQL 16 (Apache AGE) + Redis 7
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
├── docker-compose.yml        # Dev services (PostgreSQL + AGE, Redis)
└── Makefile                  # Common commands (build, test, format, dev)
```

## Documentation

| Document | Description |
|----------|-------------|
| [Coding Standards](docs/CODING_STANDARDS.md) | Style, dependency rule, testing |
| [Contributing](CONTRIBUTING.md) | Setup, workflow, PR process |
| [ADRs](architecture/adrs/) | Architecture Decision Records |

## Status

Pre-alpha. Spring Boot scaffold with domain model (Requirement + RequirementRelation), JML contracts, hand-rolled state machine, exception hierarchy, Flyway migrations, and ArchUnit architecture tests. See [CHANGELOG.md](CHANGELOG.md).

## License

MIT — see [LICENSE](LICENSE).
