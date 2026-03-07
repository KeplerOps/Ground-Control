---
title: "Implement user model and repository"
labels: [data-model, backend, auth]
phase: 1
priority: P0
---

## Description

Create the user entity model, repository, and schemas. Users are tenant-scoped and support multiple authentication providers (local, SAML, OIDC).

## References

- Data Model: Section 2.2 (User)
- User Stories: US-7.2 (Manage Users and Roles)
- PRD: Section 2 (Personas — P1 through P7)

## Acceptance Criteria

- [ ] SQLAlchemy model: `User` with all fields from data model spec
- [ ] Alembic migration creating `users` table with indexes
- [ ] Repository: `UserRepository` with:
  - `create(user_data) → User`
  - `get_by_id(user_id) → User | None`
  - `get_by_email(tenant_id, email) → User | None`
  - `list(tenant_id, filters, pagination) → list[User], total`
  - `update(user_id, updates) → User`
  - `deactivate(user_id) → User` (soft-delete, preserves data)
- [ ] Pydantic schemas: `UserCreate`, `UserRead`, `UserUpdate`, `UserSummary`
- [ ] Email validation (format + uniqueness within tenant)
- [ ] Password field NEVER included in `UserRead` schemas
- [ ] `external_id` for IdP subject identifier (SAML/OIDC)
- [ ] Unique constraint: `(tenant_id, email)`
- [ ] Unit tests for all repository methods
- [ ] Contracts: user email must be non-empty, status must be valid enum value
