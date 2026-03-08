# Ground Control — Open IT Risk Management Platform

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Ground Control is neurosymbolic constraint infrastructure, dogfooded on itself. Django backend, PostgreSQL, Redis. Pre-alpha.

## Quick Start

```bash
cp .env.example .env
make up                            # PostgreSQL 16 + Redis 7
make install                       # Python dependencies
cd backend && . .venv/bin/activate
python manage.py migrate
make dev                           # Django dev server on :8000
```

## Repository Structure

```
Ground-Control/
├── backend/                  # Django backend (Python 3.12+)
│   ├── src/ground_control/   # Application source
│   └── tests/
├── architecture/
│   ├── adrs/                 # Architecture Decision Records
│   └── design/               # Index to archived design specs
├── archive/                  # Pre-pivot docs, tools, old ADRs
├── docs/                     # Operational documentation
│   ├── architecture/         # Current architecture
│   ├── deployment/           # Dev environment setup
│   └── CODING_STANDARDS.md
└── docker-compose.yml        # Dev services (PostgreSQL, Redis)
```

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/architecture/ARCHITECTURE.md) | Current stack, project structure, config |
| [Dev Environment](docs/deployment/DEPLOYMENT.md) | Docker Compose setup, env vars, Makefile |
| [Coding Standards](docs/CODING_STANDARDS.md) | Style, dependency rule, testing |
| [Contributing](CONTRIBUTING.md) | Setup, workflow, PR process |
| [ADRs](architecture/adrs/) | Architecture Decision Records |
| [Design Specs](architecture/design/) | Target-state design (PRD, API spec, data model) |

## Status

Pre-alpha. Django project skeleton with django-ninja API mount. No domain models, API endpoints, or business logic yet. See [CHANGELOG.md](CHANGELOG.md).

## License

MIT — see [LICENSE](LICENSE).
