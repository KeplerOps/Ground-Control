# Ground Control — Architecture

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Python 3.12+ |
| Framework | Django 5.x + django-ninja |
| Database | PostgreSQL 16 |
| Multi-tenancy | django-tenants (schema-per-tenant) |
| Cache / Queue | Redis 7 (via django-q2) |
| Audit trail | django-auditlog |
| Auth | django-oauth-toolkit |
| Object storage | django-storages (S3-compatible) |
| Validation | Pydantic + pydantic-settings |
| Logging | structlog |

See [ADR-010](../../architecture/adrs/010-evaluate-django-framework.md) for the Django decision rationale.

## Project Structure

```
backend/src/ground_control/
├── settings/         # Django settings (base, test)
│   └── base.py       # Env-driven config via pydantic-settings (GC_ prefix)
├── domain/           # Django models, business logic
├── api/              # django-ninja route handlers
├── infrastructure/   # External adapters (S3, Redis, etc.)
├── schemas/          # Pydantic request/response models
├── middleware/        # Tenant resolution, auth, request-id
├── events/           # Domain event types and handlers
├── exceptions/       # Exception hierarchy
├── logging/          # Structured logging setup
├── urls.py           # Root URL config
├── asgi.py           # ASGI entry point
└── wsgi.py           # WSGI entry point
```

Most directories currently contain only `__init__.py`. Implementation is in progress.

## Dependency Rule

```
api/ → domain/ ← infrastructure/
```

- `domain/` has no imports from `api/` or `infrastructure/`
- `api/` depends on `domain/` and `schemas/`
- `infrastructure/` implements interfaces defined in `domain/`
- Enforced by `import-linter` in CI (not yet configured)

See [ADR-008](../../architecture/adrs/008-clean-architecture.md).

## Multi-Tenancy

django-tenants provides schema-per-tenant isolation. Configured in `base.py`:

- `TENANT_MODEL = "domain.Tenant"`
- `TENANT_DOMAIN_MODEL = "domain.Domain"`
- `ENGINE = "django_tenants.postgresql_backend"`
- `DATABASE_ROUTERS = ("django_tenants.routers.TenantSyncRouter",)`

See [ADR-006](../../architecture/adrs/006-multi-tenancy-strategy.md).

## Configuration

All settings load from environment variables with the `GC_` prefix via `pydantic-settings`:

```python
class GroundControlSettings(BaseSettings):
    secret_key: str = "insecure-change-me-in-production"
    debug: bool = False
    database_url: str = "postgres://localhost:5432/ground_control"
    redis_url: str = "redis://localhost:6379/0"
    model_config = {"env_prefix": "GC_"}
```

`database_url` is parsed at startup to populate Django's `DATABASES` dict.

## Background Tasks

django-q2 is configured with Redis as the broker:

```python
Q_CLUSTER = {
    "name": "ground-control",
    "workers": 4,
    "recycle": 500,
    "timeout": 60,
    "redis": env.redis_url,
}
```

Worker not yet deployed. Will run via `python manage.py qcluster`.

## What Exists vs. What Doesn't

**Exists:** Django project skeleton, settings, docker-compose dev environment, django-tenants configuration, django-ninja API mount (empty), Makefile, CI (ruff, mypy, pytest, SonarCloud).

**Does not exist yet:** Domain models (beyond Tenant/Domain), API endpoints, business logic, auth flows, plugin system, frontend, search, object storage integration, production deployment.

For the target design, see [design specifications](../../architecture/design/).
