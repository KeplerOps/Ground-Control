# Contributing to Ground Control

## Getting Started

### Prerequisites

- Python 3.12+
- Docker Engine 24+ and Docker Compose v2
- [uv](https://docs.astral.sh/uv/) (recommended) or pip

### Local Development Setup

```bash
# 1. Clone and branch
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
git checkout -b feature/your-feature dev

# 2. Start PostgreSQL and Redis
cp .env.example .env
make up

# 3. Install Python dependencies
make install

# 4. Run migrations and start the server
cd backend && . .venv/bin/activate
python manage.py migrate
python manage.py runserver 0.0.0.0:8000
# Or from the project root: make dev
```

### Useful Makefile Targets

| Target | Description |
|--------|-------------|
| `make up` | Start Docker Compose services (PostgreSQL, Redis) |
| `make down` | Stop Docker Compose services |
| `make dev` | Start Django development server |
| `make install` | Create venv and install dependencies |
| `make lint` | Run ruff + mypy |
| `make test` | Run pytest |
| `make help` | Show all targets |

## Branch Strategy

- `main` — production-ready, protected
- `dev` — integration branch, all PRs target this
- `feature/*` — feature branches, branched from `dev`

## Coding Standards

Read [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) for the complete reference. Key points below.

### Python Backend

| Tool | Purpose | Command |
|------|---------|---------|
| `ruff check` | Linting | `cd backend && ruff check src/` |
| `ruff format` | Formatting | `cd backend && ruff format src/` |
| `mypy` | Type checking | `cd backend && mypy src/` |
| `pytest` | Testing | `cd backend && pytest` |

- **Line length**: 100
- **Type hints**: Required on all public functions and methods
- **Docstrings**: Google style, on public API boundaries only
- **Imports**: stdlib, third-party, local (enforced by ruff)
- **No `Any`** unless unavoidable (comment why)
- **No `print()`** — use `structlog`

### TypeScript Frontend

| Tool | Purpose | Command |
|------|---------|---------|
| `biome check` | Lint + format | `cd frontend && npx biome check src/` |
| `tsc` | Type checking | `cd frontend && npx tsc --noEmit` |

- **Strict mode**: `strict: true` in `tsconfig.json`
- **No `any`** — use `unknown` and narrow

### Naming Conventions (Python)

| Element | Convention | Example |
|---------|-----------|---------|
| Modules | `snake_case` | `risk_service.py` |
| Classes | `PascalCase` | `RiskService` |
| Functions/methods | `snake_case` | `create_risk()` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Private | `_leading_underscore` | `_validate_score()` |

## Architecture Rules

The dependency rule is enforced by `import-linter` in CI:

```
api/ -> domain/ <- infrastructure/
```

- `domain/` has **zero** framework imports beyond Django models (no views, no URL routing)
- `api/` depends on `domain/` and `schemas/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`

See [ADR-008](architecture/adrs/008-clean-architecture.md) for rationale.

## Commit Messages

- Imperative mood: `Add risk scoring engine` not `Added risk scoring engine`
- Every commit updates `CHANGELOG.md`

## Pull Requests

- Target `dev`, not `main`
- PRs require passing CI (lint + typecheck + tests + import-linter)
- No coverage regression
- Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md)

## Testing

```
tests/
├── unit/          # Domain logic only. No DB, no HTTP.
├── integration/   # With real PostgreSQL (testcontainers).
└── e2e/           # Playwright, full stack.
```

- Test names describe behavior: `test_create_risk_fails_when_likelihood_out_of_range`
- Coverage minimums: 80% domain, 70% api/infrastructure
- Tests are independent — no shared mutable state
