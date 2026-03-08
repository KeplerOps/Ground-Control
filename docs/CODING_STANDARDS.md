# Ground Control — Coding Standards

## Project Structure

```
backend/src/ground_control/
├── api/              # Route handlers only. No business logic.
├── domain/           # Django models, services, use case functions
├── infrastructure/   # External adapters (S3, Redis, external APIs)
├── schemas/          # Pydantic request/response models
├── middleware/        # Auth, request-id
├── events/           # Domain event bus and handlers
├── exceptions/       # Exception hierarchy
├── logging/          # Structured logging setup
├── settings/         # Django settings
├── urls.py           # Root URL config
├── asgi.py           # ASGI entry point
└── wsgi.py           # WSGI entry point
```

## Dependency Rule

```
api/ → domain/ ← infrastructure/
```

- `domain/` imports nothing from `api/` or `infrastructure/` (except `django.db.models`)
- `api/` depends on `domain/` and `schemas/`. Never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`
- `schemas/`, `exceptions/`, `logging/`, `events/` are cross-cutting — importable by any layer

Enforced by `import-linter` in CI.

## Python Style

| Tool | Purpose | Command |
|------|---------|---------|
| `ruff format` | Formatting | `cd backend && ruff format src/ tests/` |
| `ruff check` | Linting | `cd backend && ruff check src/ tests/` |
| `mypy` | Type checking | `cd backend && mypy src/` |

- Line length: 100
- Type annotations on all public functions. No `Any` unless unavoidable (comment why).
- Docstrings: Google style, on public API boundaries only. Code should be self-explanatory.
- Imports: stdlib, third-party, local (enforced by ruff isort).
- No `print()`. Use `structlog`.

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Modules | `snake_case` | `risk_service.py` |
| Classes | `PascalCase` | `RiskService` |
| Functions/methods | `snake_case` | `create_risk()` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Private | `_leading_underscore` | `_validate_score()` |

## Exceptions

All application exceptions inherit from a base hierarchy in `exceptions/`. Domain layer raises these. API layer maps them to HTTP responses via middleware.

- Never raise `HTTPException` from the domain layer
- Never catch `Exception` broadly
- Wrap external library exceptions in `infrastructure/`

## Logging

Use `structlog` with semantic event names:

```python
import structlog
logger = structlog.get_logger()
logger.info("risk_created", risk_id=risk.id, tenant_id=tenant_id)
```

- Never log secrets, tokens, passwords, or PII
- Always include `tenant_id` and `actor_id` when available

## Testing

```
tests/
├── unit/          # Domain logic only. No DB, no HTTP.
├── integration/   # With real PostgreSQL (testcontainers).
└── e2e/           # Playwright, full stack.
```

- Test names describe behavior: `test_create_risk_fails_when_likelihood_out_of_range`
- Tests are independent — no shared mutable state
- Coverage minimums: 80% domain, 70% api/infrastructure

## Git & CI

- All code goes through PR targeting `dev`. No direct push to `main` or `dev`.
- PRs require: passing CI (lint + typecheck + tests), no coverage regression.
- Commit messages: imperative mood. `Add risk scoring engine` not `Added risk scoring engine`.
- CI pipeline: `ruff check` → `ruff format --check` → `mypy` → `pytest` → coverage report.
