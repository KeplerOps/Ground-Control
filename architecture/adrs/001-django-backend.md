# ADR-001: Python 3.12+ with Django and django-ninja for Backend

## Status

Superseded by [ADR-013](013-java-spring-boot-rewrite.md)

## Date

2026-03-08

## Context

Ground Control needs a backend framework. The project previously selected FastAPI (ADR-001, archived) but switched to Django (ADR-010, archived) for its batteries-included approach: ORM, migrations, admin, auth, and a mature ecosystem. That decision stands and is reaffirmed here as the starting point for the new project frame.

django-ninja provides FastAPI-style route declarations and Pydantic schema integration on top of Django, giving us type-safe APIs without leaving the Django ecosystem.

## Decision

- Python 3.12+ as the runtime (target 3.12 and 3.13)
- Django as the web framework and ORM
- django-ninja for API layer (Pydantic schemas, OpenAPI generation)
- pydantic-settings for configuration management (env vars with `GC_` prefix)
- structlog for structured logging
- django-q2 for background task processing
- django-auditlog for audit trail
- django-storages with S3 backend for file storage
- hatchling as the build backend

## Consequences

### Positive

- Django ORM + migrations eliminate manual schema management
- django-ninja gives us type-safe APIs with automatic OpenAPI docs
- Large ecosystem of battle-tested Django packages
- Built-in admin interface for debugging and operations

### Negative

- Django's sync-first model limits async throughput (acceptable for this workload)
- django-ninja is smaller community than DRF

### Risks

- django-ninja is less mature than Django REST Framework, but its Pydantic integration aligns better with our type-safety goals
