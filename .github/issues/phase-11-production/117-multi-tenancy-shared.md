---
title: "Implement multi-tenancy enforcement (shared schema + RLS)"
labels: [backend, multi-tenancy, security, production]
phase: 11
priority: P0
---

## Description

Harden and verify the shared-schema multi-tenancy model with comprehensive RLS enforcement, tenant resource limits, and cross-tenant isolation testing.

## References

- Architecture: Section 8.2 (Multi-Tenancy Models)
- Deployment: Section 6 (Multi-Tenancy Configuration)
- Issue #026 (Tenant Model)

## Acceptance Criteria

- [ ] RLS enabled and verified on ALL tenant-scoped tables
- [ ] Cross-tenant isolation tests: create data as tenant A, verify invisible to tenant B
- [ ] Tenant resource limits: max users, max artifacts, max storage
- [ ] Tenant configuration: per-tenant settings (taxonomy, SSO, plugins)
- [ ] Tenant provisioning CLI: `gc-admin create-tenant --name "..." --slug "..."`
- [ ] Tenant suspension: disable all access, preserve data
- [ ] Tenant data export: full tenant data dump (for portability)
- [ ] Integration tests covering all entity types for isolation
