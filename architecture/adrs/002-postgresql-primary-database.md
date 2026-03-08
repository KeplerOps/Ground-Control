# ADR-002: PostgreSQL 16+ as Primary Database

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control requires a database that supports multi-tenancy with row-level security, JSONB for flexible attributes, full-text search, strong ACID guarantees for audit log integrity, and is self-hostable without licensing costs.

We evaluated: PostgreSQL, MySQL/MariaDB, MongoDB, and CockroachDB.

## Decision

Use PostgreSQL 16+ as the sole primary database.

- Row-Level Security (RLS) for tenant isolation at the database level
- JSONB columns for flexible/extensible attributes without schema migrations
- Built-in tsvector/tsquery for full-text search (avoids Elasticsearch dependency for small deployments)
- Append-only audit log with hash chains for tamper detection
- Mature ecosystem with Django ORM and django-tenants support
- Optional upgrade path to Citus for horizontal sharding

## Consequences

### Positive

- Single database simplifies deployment and operations
- RLS provides defense-in-depth for tenant isolation
- JSONB enables plugin-defined custom fields without migrations
- tsvector eliminates the need for a separate search service at small scale
- Strong ACID guarantees protect audit log integrity

### Negative

- Single database is a scaling bottleneck (mitigated: read replicas, Citus, or database-per-tenant mode)
- RLS adds complexity to query debugging
- JSONB queries can be slower than dedicated document stores for complex queries

### Risks

- Must ensure RLS policies are correctly applied to every table — a missed policy is a data leak
