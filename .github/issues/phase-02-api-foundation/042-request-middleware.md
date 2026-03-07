---
title: "Implement request ID, correlation, and tenant context middleware"
labels: [api, backend, cross-cutting, observability]
phase: 2
priority: P0
---

## Description

Implement middleware components that establish request context (request ID, tenant, correlation ID) for every inbound request. This context flows through logging, audit trails, and error responses.

## References

- Architecture: Section 3.1 (API Gateway — request routing)
- Issue #018 (Structured Logging — context variables)
- API Spec: Section 2.2 (X-Tenant-ID header)

## Acceptance Criteria

- [ ] **Request ID middleware:**
  - Extracts `X-Request-ID` header or generates UUID
  - Sets in `contextvars` for logging
  - Returns `X-Request-ID` in response headers
- [ ] **Tenant context middleware:**
  - Extracts `X-Tenant-ID` from header (required for multi-tenant, optional for single-tenant)
  - Validates tenant exists and is active
  - Sets PostgreSQL session variable: `SET app.current_tenant_id = '{uuid}'`
  - Sets in `contextvars` for logging and downstream access
  - Returns 400 if tenant ID missing (multi-tenant mode)
  - Returns 404 if tenant not found or suspended
- [ ] **Correlation ID middleware:**
  - Extracts or generates `X-Correlation-ID` for cross-service tracing
  - Propagated to outbound HTTP calls
- [ ] All context variables accessible via `get_request_context()` helper
- [ ] Unit tests for each middleware (valid, missing, invalid inputs)
