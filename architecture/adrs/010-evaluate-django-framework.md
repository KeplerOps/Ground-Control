# ADR-010: Switch to Django + django-ninja for Backend (Supersedes ADR-001)

## Status

Accepted

## Date

2026-03-08

## Context

ADR-001 selected Python 3.12+ with FastAPI as the backend framework. Before any application code was written, we re-evaluated this decision in light of Ground Control's actual requirements.

Ground Control is an IT Risk Management (ITRM) platform. The core workload is CRUD-heavy, permissions-heavy, and admin-heavy: managing risk assessments, control testing, evidence collection, and compliance reporting across multiple tenants. This profile strongly favors a batteries-included framework.

The FastAPI stack (FastAPI + SQLAlchemy + Alembic + asyncpg + passlib + python-jose) requires assembling 8-10 separate libraries and writing significant glue code to achieve what Django provides out of the box. Since zero application code exists, the switching cost is effectively zero.

We evaluated: Django + django-ninja, Django + DRF, and the current FastAPI stack.

### What Django provides out of the box that FastAPI requires manual assembly

| Capability | Django | FastAPI Stack |
|---|---|---|
| ORM + migrations | Built-in | SQLAlchemy + Alembic |
| Admin panel | Built-in | Must build or adopt SQLAdmin |
| Auth + permissions | Built-in (users, groups, object-level) | python-jose + passlib + manual |
| Session/token management | Built-in | Manual |
| Security middleware (CSRF, CORS, etc.) | Built-in | Manual |
| Multi-tenancy | django-tenants (mature) | Manual middleware + RLS |
| Plugin system | Django apps (migrations, signals, admin, URLs per app) | Manual plugin runtime |
| Audit logging | django-auditlog | Manual |
| Background tasks | django-q2, celery | Manual with Redis/arq |
| Test framework | Built-in test client, fixtures | pytest + httpx |

### Addressing ADR-001's arguments for FastAPI

- **"Native async/await"**: Django 4.1+ has async views; Django 5.x has async ORM support. django-ninja is fully async. ITRM workloads are not latency-critical real-time systems — async is a nice-to-have, not a deciding factor.
- **"Auto OpenAPI from Pydantic"**: django-ninja provides the identical developer experience — Pydantic v2 models, auto-generated OpenAPI 3.1 schema, type-safe request/response validation.
- **"Dependency injection"**: Django provides middleware, signals, and the app registry. DI is a design pattern achievable in any framework.
- **"FastAPI is lighter"**: Only until you add the 10 libraries needed to match Django's functionality — then you're maintaining more code, not less.

### Proven scale

Django powers Instagram (2B+ users, originally built by 3 engineers), Pinterest, Dropbox, and Mozilla. It is battle-tested at scales far beyond Ground Control's requirements.

## Decision

Switch to Django 5.x with django-ninja as the backend framework. This supersedes ADR-001.

- **Django 5.x**: ORM, migrations, admin, auth, permissions, middleware, signals, test framework
- **django-ninja**: Pydantic v2 request/response validation, auto OpenAPI 3.1 schema generation, async view support — preserving the API-first DX that motivated FastAPI
- **psycopg 3**: Async-capable PostgreSQL driver (replaces asyncpg)
- **django-tenants**: Multi-tenancy with schema-per-tenant support (aligns with ADR-006)
- **django-auditlog**: Audit trail for all model changes
- **django-storages**: S3/GCS/Azure storage backend for evidence artifacts
- **django-oauth-toolkit**: OAuth2 provider for agent authentication (aligns with ADR-007)
- **django-q2**: Background task processing via Redis

Retained from original stack: pydantic, pydantic-settings, structlog, redis, boto3.

Removed: fastapi, uvicorn, starlette, sqlalchemy, alembic, asyncpg, python-jose, passlib.

## Consequences

### Positive

- Massive reduction in glue code — Django provides ORM, auth, admin, migrations, middleware out of the box
- Django admin gives internal ops/support a full management UI with zero custom development
- Django apps are a natural plugin system, directly supporting ADR-004's plugin architecture
- django-ninja preserves the Pydantic + OpenAPI DX that motivated FastAPI in ADR-001
- Battle-tested at scale (Instagram, Pinterest) — no risk of outgrowing the framework
- Larger talent pool and ecosystem for GRC-adjacent needs
- django-tenants is a mature, purpose-built multi-tenancy solution
- Faster time to first feature — less infrastructure to build before delivering value

### Negative

- Django's ORM is less flexible than SQLAlchemy for complex query composition (mitigated: raw SQL escape hatch, django ORM covers 95% of ITRM queries)
- Django has opinions about project structure that may conflict with pure clean architecture layering (mitigated: ADR-008's layers map well to Django apps)
- Async ORM support is newer and less battle-tested than sync (mitigated: sync-first is fine for ITRM workloads)

### Risks

- django-tenants requires PostgreSQL schemas, which adds migration complexity for multi-tenant deployments (mitigated: well-documented, widely used in SaaS)
- django-ninja is smaller community than DRF (mitigated: growing fast, Pydantic-based, easy to swap to DRF if needed)
