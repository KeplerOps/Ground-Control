---
title: "Implement tenant model and row-level security"
labels: [data-model, backend, multi-tenancy, security]
phase: 1
priority: P0
---

## Description

Create the tenant entity model and implement PostgreSQL Row-Level Security (RLS) policies for tenant isolation. This is the foundational isolation mechanism — every data access must be scoped to a tenant.

## References

- Data Model: Section 2.1 (Tenant)
- Data Model: Section 4.1 (RLS policies)
- Architecture: Section 8.2 (Multi-Tenancy Models)
- User Stories: US-7.5 (Manage Taxonomy & Configuration — tenant settings)

## Acceptance Criteria

- [ ] SQLAlchemy model: `Tenant` with fields per data model spec (id, name, slug, settings, status, timestamps)
- [ ] Alembic migration creating `tenants` table
- [ ] RLS policy: `CREATE POLICY tenant_isolation ON <table> USING (tenant_id = current_setting('app.current_tenant_id')::UUID)`
- [ ] RLS enablement for all tenant-scoped tables (applied as tables are created)
- [ ] Session-level tenant setting via `SET app.current_tenant_id = '{uuid}'`
- [ ] Repository: `TenantRepository` with CRUD operations
- [ ] Pydantic schemas: `TenantCreate`, `TenantRead`, `TenantUpdate`
- [ ] Tenant slug validation (lowercase alphanumeric + hyphens, unique)
- [ ] Contracts: `@icontract.ensure(lambda result: result.slug == result.slug.lower())`
- [ ] Unit tests including RLS isolation verification (insert as tenant A, query as tenant B → empty)

## Technical Notes

- RLS is enforced at the database level — even raw SQL queries respect tenant boundaries
- The middleware (#042) sets `app.current_tenant_id` at the start of each request
- For schema-per-tenant mode, RLS is replaced by schema switching
- Consider a `system` tenant for global resources (CCL, framework definitions)
