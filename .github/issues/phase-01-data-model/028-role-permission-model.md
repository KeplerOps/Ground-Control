---
title: "Implement role and permission model"
labels: [data-model, backend, auth, security]
phase: 1
priority: P0
---

## Description

Create the role, permission, and user-role assignment models. Roles define permission sets, users are assigned roles, and permissions follow the `resource:action:scope` format.

## References

- Data Model: Section 2.3 (Role & Permission, user_roles)
- Architecture: Section 4 (Authorization Model — RBAC + ABAC)
- User Stories: US-7.2 (Manage Users and Roles)

## Acceptance Criteria

- [ ] SQLAlchemy models: `Role`, `UserRole`
- [ ] Alembic migration creating `roles` and `user_roles` tables
- [ ] System (built-in) roles seeded via migration:
  - `admin` — full access
  - `risk_manager` — risks:*, controls:read, assessments:read, reports:*
  - `auditor` — assessments:*, controls:*, findings:*, artifacts:*
  - `control_owner` — controls:read, artifacts:write (own controls), evidence_requests:respond
  - `compliance_analyst` — controls:*, frameworks:*, ccl:*
  - `viewer` — *:read
  - `agent` — assessments:read, test_procedures:write, artifacts:write
- [ ] Permission format: `resource:action:scope` stored as JSONB array
- [ ] `UserRole` junction table with optional `scope` (JSONB for ABAC: `{"business_unit": "eng"}`)
- [ ] Repository: `RoleRepository` with CRUD + permission checking
- [ ] Pydantic schemas: `RoleCreate`, `RoleRead`, `PermissionSet`
- [ ] `is_system` flag on built-in roles prevents deletion
- [ ] Unit tests for permission resolution (user → roles → permissions)
- [ ] Contracts: system roles cannot be deleted, role names unique per tenant

## Technical Notes

- Permissions are evaluated by the policy engine (#025) not hardcoded if/else
- `scope` on `UserRole` enables ABAC: same role can be scoped to different BUs
- Consider caching resolved permissions in Redis (invalidate on role change)
