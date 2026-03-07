---
title: "Implement shared exception hierarchy"
labels: [foundation, backend, cross-cutting]
phase: 0
priority: P0
---

## Description

Design and implement a structured exception hierarchy for the Python backend. Exceptions should be domain-aware, carry error codes, and map cleanly to HTTP status codes. This enables consistent error handling across all services and API endpoints.

## References

- API Spec: Section 3.3 (Error Response format)
- Architecture: Section 3.4 (Domain Services)

## Acceptance Criteria

- [ ] `backend/src/ground_control/exceptions/__init__.py` with hierarchy:
  ```
  GroundControlError (base)
  ├── ValidationError          → 422
  │   ├── SchemaValidationError
  │   └── BusinessRuleError
  ├── AuthenticationError      → 401
  │   ├── InvalidCredentialsError
  │   ├── TokenExpiredError
  │   └── MFARequiredError
  ├── AuthorizationError       → 403
  │   ├── InsufficientPermissionsError
  │   └── TenantAccessDeniedError
  ├── NotFoundError            → 404
  │   └── EntityNotFoundError(entity_type, entity_id)
  ├── ConflictError            → 409
  │   ├── DuplicateEntityError
  │   └── OptimisticLockError
  ├── RateLimitError           → 429
  ├── ExternalServiceError     → 502
  │   ├── StorageError
  │   ├── SearchIndexError
  │   └── PluginError
  └── InternalError            → 500
  ```
- [ ] Each exception carries:
  - `error_code: str` — machine-readable code (e.g., `"entity_not_found"`)
  - `message: str` — human-readable message
  - `details: list[dict]` — optional field-level details
  - `status_code: int` — HTTP status code
- [ ] Global exception handler in FastAPI that converts exceptions to API error response format
- [ ] Exceptions are logged at appropriate levels (4xx → WARNING, 5xx → ERROR)
- [ ] Sensitive information (stack traces, internal paths) stripped from production error responses
- [ ] Unit tests for exception hierarchy and handler

## Technical Notes

- Use `@app.exception_handler(GroundControlError)` in FastAPI
- Also handle `RequestValidationError` (Pydantic) and `HTTPException` (Starlette) for uniform output
- Include `request_id` in error responses for correlation
- Never expose raw database errors to clients
