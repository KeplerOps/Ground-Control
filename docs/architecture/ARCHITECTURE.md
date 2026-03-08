# Ground Control — Architecture

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Python 3.12+ |
| Framework | Django 5.x + django-ninja |
| Database | PostgreSQL 16 + Apache AGE 1.6.0 (graph) |
| Cache / Queue | Redis 7 (via django-q2) |
| Audit trail | django-auditlog |
| Object storage | django-storages (S3-compatible) |
| Validation | Pydantic + pydantic-settings |
| Logging | structlog |
| Contracts | icontract |
| WSGI server | gunicorn |
| Container | Docker (multi-stage, non-root) |
| Registry | GHCR (`ghcr.io/keplerops/ground-control`) |

See [ADR-001](../../architecture/adrs/001-django-backend.md) for the Django decision rationale.

## Project Structure

```
backend/src/ground_control/
├── settings/         # Django settings (base, test)
│   └── base.py       # Env-driven config via pydantic-settings (GC_ prefix)
├── domain/           # Django models, business logic
├── api/              # django-ninja route handlers
├── infrastructure/   # External adapters (S3, Redis, etc.)
├── schemas/          # Pydantic request/response models
├── middleware/        # Auth, request-id
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

django-q2 is configured with Redis as the broker. Worker runs via `python manage.py qcluster` (not yet deployed).

## Deployment

The backend ships as a Docker image built from `backend/Dockerfile` (multi-stage, `python:3.12-slim`). The runtime stage runs as non-root user `gc` (UID 1000) with gunicorn as the WSGI server.

Static files are collected at build time. Migrations run as a separate init step — they are not baked into the image startup.

The `docker.yml` workflow builds and publishes to GHCR on push to `main` or semver tags. CI must pass first (reusable workflow gate).

## What Exists vs. What Doesn't

**Exists:** Django project skeleton, settings, docker-compose dev environment, django-ninja API (with shared exception handler), structured logging, Makefile, CI (ruff, mypy, pytest, SonarCloud), production Dockerfile, GHCR publishing.

**Does not exist yet:** Domain models, API endpoints, business logic, auth flows, frontend, search, object storage integration.

For pre-pivot design artifacts, see `archive/docs/`.
