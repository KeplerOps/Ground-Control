# ADR-006: Multi-Tenancy Strategy (Shared Schema Default)

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control must support multiple organizations (tenants) in a single deployment for managed/SaaS scenarios, while also supporting single-tenant self-hosted deployments. Tenant data isolation is critical — cross-tenant data leakage is a security incident.

We evaluated: shared schema with RLS, schema-per-tenant, database-per-tenant, and application-level filtering only.

## Decision

Default to shared-schema multi-tenancy with PostgreSQL Row-Level Security (RLS), with configurable upgrade paths.

- Every table includes a `tenant_id` column
- PostgreSQL RLS policies enforce isolation at the database level
- Application code also filters by `tenant_id` (defense in depth)
- Tenant context is set via middleware on every request
- Configuration supports three modes:
  - **Shared schema** (default): all tenants in one schema, RLS enforced
  - **Schema-per-tenant**: each tenant gets a dedicated PostgreSQL schema
  - **Database-per-tenant**: each tenant gets a dedicated database instance

## Consequences

### Positive

- Shared schema is simplest to operate — one migration, one connection pool
- RLS provides database-level enforcement independent of application bugs
- Defense in depth: even if application code has a bug, RLS prevents cross-tenant access
- Upgrade path to schema/database isolation for high-security tenants

### Negative

- Shared schema has noisy-neighbor risk (one tenant's heavy query affects others)
- RLS policies must be applied to every table — a missed table is a vulnerability
- Schema/database-per-tenant modes add operational complexity (migrations, connection management)

### Risks

- RLS performance overhead on large tables (mitigated: proper indexing on `tenant_id`)
- Must test tenant isolation rigorously — this is a compliance-critical invariant
