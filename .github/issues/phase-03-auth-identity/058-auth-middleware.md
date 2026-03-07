---
title: "Implement authorization middleware and decorators"
labels: [backend, auth, security, api]
phase: 3
priority: P0
---

## Description

Create FastAPI middleware and route decorators that enforce authorization on every endpoint. Connect the RBAC/ABAC engines to the API layer.

## References

- Issue #056 (RBAC Engine), #057 (ABAC Engine)
- API Spec: Section 2 (Authentication — bearer tokens)

## Acceptance Criteria

- [ ] `require_permission(resource, action)` decorator for route handlers:
  ```python
  @router.post("/risks")
  @require_permission("risks", "write")
  async def create_risk(...): ...
  ```
- [ ] `require_any_permission(...)` and `require_all_permissions(...)` variants
- [ ] `require_role(role_name)` shorthand decorator
- [ ] Authorization middleware evaluates RBAC + ABAC for every request
- [ ] 403 response with clear error message when denied
- [ ] Authorization decisions audit-logged for sensitive operations
- [ ] Bypass for system/internal calls (e.g., background jobs)
- [ ] Unit and integration tests for all permission scenarios
