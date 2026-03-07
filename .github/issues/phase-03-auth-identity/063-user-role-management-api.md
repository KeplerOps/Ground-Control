---
title: "Implement user and role management API"
labels: [api, backend, auth, admin]
phase: 3
priority: P0
---

## Description

Build admin API endpoints for managing users, roles, and role assignments.

## References

- API Spec: Section 4.9 (Users implied)
- User Stories: US-7.2 (Manage Users and Roles)

## Acceptance Criteria

- [ ] User endpoints:
  - `GET /api/v1/users` — list (filterable, paginated)
  - `POST /api/v1/users` — create (admin or SCIM)
  - `GET /api/v1/users/{id}` — get user details
  - `PATCH /api/v1/users/{id}` — update
  - `POST /api/v1/users/{id}/deactivate` — deactivate
  - `POST /api/v1/users/{id}/reactivate` — reactivate
  - `GET /api/v1/users/{id}/roles` — list role assignments
  - `POST /api/v1/users/{id}/roles` — assign role
  - `DELETE /api/v1/users/{id}/roles/{role_id}` — unassign role
- [ ] Role endpoints:
  - `GET /api/v1/roles` — list
  - `POST /api/v1/roles` — create custom role
  - `PATCH /api/v1/roles/{id}` — update permissions
  - `DELETE /api/v1/roles/{id}` — delete (only custom, not system)
- [ ] All mutations require admin role
- [ ] All mutations audit-logged
- [ ] Integration tests
