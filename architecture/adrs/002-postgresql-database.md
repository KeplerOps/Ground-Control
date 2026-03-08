# ADR-002: PostgreSQL as Primary Database

## Status

Accepted

## Date

2026-03-08

## Context

Ground Control needs a relational database. PostgreSQL is already configured in the Django settings via psycopg and django-tenants' PostgreSQL backend. This reaffirms the earlier decision (ADR-002, archived).

## Decision

- PostgreSQL as the sole primary database
- psycopg (v3) as the database adapter
- Django ORM for all query construction and migrations
- Redis for caching and task queue backend (django-q2)

## Consequences

### Positive

- PostgreSQL's JSONB, full-text search, and advanced indexing cover foreseeable needs without additional datastores
- django-tenants provides schema-based multi-tenancy on PostgreSQL
- Mature tooling for backups, replication, and monitoring

### Negative

- PostgreSQL-specific features (if used) reduce portability to other databases

### Risks

- None significant — PostgreSQL is the standard choice for Django projects
