# Ground Control — Coding Standards

## Project Structure

```
backend/src/ground_control/
├── api/              # Route handlers only. No business logic.
├── domain/           # Django models, services, use case functions
├── infrastructure/   # External adapters (S3, Redis, external APIs)
├── schemas/          # Pydantic request/response models
├── middleware/        # Custom middleware
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

## Schemas & Response Format

All project schemas inherit from `BaseSchema` (`ground_control.schemas.base`), which extends `ninja.Schema` and provides a single point for future shared configuration.

**Naming convention:** `FooIn` for input schemas, `FooOut` for output schemas.

### Success responses

Single-resource endpoints return flat data (no envelope wrapper) — this is django-ninja idiomatic:

```python
@router.get("/{id}", response=RiskOut)
def get_risk(request, id: int) -> Risk:
    return Risk.objects.get(id=id)
```

### Paginated responses

List endpoints use `GroundControlPagination`, which produces:

```json
{
  "items": [...],
  "meta": {
    "total_count": 100,
    "page": 1,
    "per_page": 20,
    "total_pages": 5
  }
}
```

Usage:

```python
from ground_control.schemas import GroundControlPagination

@router.get("/", response=list[RiskOut])
@paginate(GroundControlPagination)
def list_risks(request):
    return Risk.objects.all()
```

### Error responses

All errors use the nested format:

```json
{
  "error": {
    "code": "not_found",
    "message": "User 42 not found",
    "detail": null
  }
}
```

The `code` field is a machine-readable identifier (e.g. `not_found`, `validation_error`). The `message` field is a human-readable description. The `detail` field is an optional dictionary with extra structured information.

## Exceptions

All application exceptions inherit from `GroundControlError` in `exceptions/`. Domain layer raises these. The `NinjaAPI` exception handler in `api/__init__.py` maps them to HTTP responses.

| Exception | Default `error_code` | HTTP Status |
|-----------|---------------------|-------------|
| `NotFoundError` | `not_found` | 404 |
| `DomainValidationError` | `validation_error` | 422 |
| `AuthenticationError` | `authentication_error` | 401 |
| `AuthorizationError` | `authorization_error` | 403 |
| `ConflictError` | `conflict` | 409 |
| `GroundControlError` (fallback) | `ground_control_error` | 500 |

Subclasses of a mapped exception inherit the parent's status code (MRO lookup). `DomainValidationError` is named to avoid collision with `django.core.exceptions.ValidationError`.

- Never raise HTTP-specific exceptions from the domain layer
- Never catch `Exception` broadly
- Wrap external library exceptions in `infrastructure/`

## Logging

Use `structlog` with semantic event names. The configuration lives in
`ground_control/logging/__init__.py` and is activated at settings import time.
Request context is provided by `django-structlog` middleware.

```python
import structlog

logger = structlog.get_logger()

# Simple event
logger.info("risk_created", risk_id=risk.id)

# Bind context once, carry through the call chain
logger = logger.bind(tenant_id=tenant.id, actor_id=user.id)
logger.warning("score_above_threshold", risk_id=risk.id, score=95)
```

**Output format** is selected by the `DEBUG` setting:

| Environment | `DEBUG` | Format |
|-------------|---------|--------|
| Development | `True`  | Colored console (human-readable) |
| Production  | `False` | JSON lines (machine-parseable) |

**Automatic context**: `django-structlog`'s `RequestMiddleware` automatically
binds the following fields to every log entry within an HTTP request:

| Field | Source |
|-------|--------|
| `request_id` | `X-Request-ID` header or auto-generated UUID |
| `ip` | Client IP address |
| `user_id` | Authenticated user ID (when available) |

**Service identity**: Every log entry includes `service.name` and
`service.version` fields, following
[OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/).
When adding custom bindings, prefer OTel semantic convention attribute names
where applicable.

**Rules**:

- No `print()` — use `structlog`
- Never log secrets, tokens, passwords, or PII
- Always include `tenant_id` and `actor_id` when available
- Use semantic event names (`risk_created`, not `"Created a new risk"`)

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
