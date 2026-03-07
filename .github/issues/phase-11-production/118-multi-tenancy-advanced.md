---
title: "Implement schema-per-tenant and database-per-tenant modes"
labels: [backend, multi-tenancy, production]
phase: 11
priority: P2
---

## Description

Implement the advanced multi-tenancy modes for higher isolation requirements.

## References

- Architecture: Section 8.2 (Schema per tenant, Database per tenant)
- Deployment: Section 6.2, 6.3

## Acceptance Criteria

- [ ] Schema-per-tenant: Alembic runs migrations per schema, tenant switching via `SET search_path`
- [ ] Database-per-tenant: connection routing per tenant, separate connection pools
- [ ] Mode selection via `MULTI_TENANCY_MODE` environment variable
- [ ] Tenant provisioning creates schema/database as appropriate
- [ ] Migration tooling handles all tenant schemas/databases
- [ ] Integration tests for each mode
