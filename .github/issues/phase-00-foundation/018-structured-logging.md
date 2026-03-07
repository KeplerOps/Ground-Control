---
title: "Implement structured logging framework"
labels: [foundation, backend, cross-cutting, observability]
phase: 0
priority: P0
---

## Description

Set up a structured JSON logging framework using `structlog` that provides consistent, machine-parseable log output across all backend services. Every log entry should include request context (tenant, user, request ID) for correlation and filtering.

## References

- Deployment: Section 8.3 (Logging — structured JSON format)
- Architecture: Section 6 (Security — audit logging)

## Acceptance Criteria

- [ ] `backend/src/ground_control/logging/` module:
  - `setup.py` — configures structlog with processors
  - `context.py` — context variable management (tenant_id, request_id, user_id)
  - `middleware.py` — request logging middleware
- [ ] Structlog configured with processors:
  - Add log level
  - Add timestamp (ISO 8601 UTC)
  - Add service name
  - Add context variables (tenant_id, request_id, user_id, actor_type)
  - Exception formatting (exc_info → structured)
  - JSON renderer (production) or colored console (development)
- [ ] Log output format matches deployment spec:
  ```json
  {
    "timestamp": "2026-03-07T15:30:00Z",
    "level": "info",
    "service": "gc-app",
    "tenant_id": "...",
    "request_id": "req_abc123",
    "user_id": "...",
    "message": "Risk created",
    "event_data": { "risk_id": "...", "ref_id": "RISK-001" }
  }
  ```
- [ ] Request logging middleware logs:
  - Request start: method, path, user_agent
  - Request end: method, path, status_code, duration_ms
- [ ] Log levels configurable via environment variable (`LOG_LEVEL`)
- [ ] Sensitive data never logged (passwords, tokens, secrets) — add scrubbing processor
- [ ] `get_logger()` helper that returns a bound logger with current context
- [ ] Unit tests for logging configuration and context binding

## Technical Notes

- Use `contextvars` for request-scoped context (works with asyncio)
- `structlog` integrates with stdlib logging — configure both to output consistently
- In development, use `structlog.dev.ConsoleRenderer` for human-readable output
- In production, use `structlog.processors.JSONRenderer`
- Add log sampling for high-volume events (e.g., health checks) to reduce noise
