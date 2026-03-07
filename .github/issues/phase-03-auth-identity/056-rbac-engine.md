---
title: "Implement RBAC engine with permission resolution"
labels: [backend, auth, security]
phase: 3
priority: P0
---

## Description

Build the role-based access control engine that resolves a user's effective permissions from their role assignments and evaluates access decisions.

## References

- Architecture: Section 4 (Authorization — RBAC)
- Data Model: Section 2.3 (Role & Permission)
- Issue #028 (Role & Permission Model), #025 (Policy as Code)
- User Stories: US-7.2

## Acceptance Criteria

- [ ] `PermissionResolver` service:
  - `get_effective_permissions(user_id) → set[Permission]` — resolves all permissions from all assigned roles
  - `has_permission(user_id, resource, action, scope) → bool`
  - Caches resolved permissions in Redis (invalidate on role/assignment change)
- [ ] Permission format parsing: `"risks:read:*"` → `Permission(resource="risks", action="read", scope="*")`
- [ ] Wildcard support: `*` matches all (e.g., `risks:*:*` = full access to risks)
- [ ] Scope evaluation: `bu=engineering` matches user's business unit
- [ ] System roles immutable; custom roles fully configurable
- [ ] Contracts: permission strings must match `resource:action[:scope]` pattern
- [ ] Property-based tests (Hypothesis): random permission sets always resolve deterministically
- [ ] Unit tests for resolution, wildcard matching, scope evaluation
