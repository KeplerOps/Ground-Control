---
title: "Scaffold FastAPI application with middleware stack"
labels: [api, backend, foundation]
phase: 2
priority: P0
---

## Description

Create the FastAPI application entry point with the full middleware stack, lifespan management, and dependency injection setup. This is the HTTP server scaffold that all API endpoints will be mounted on.

## References

- Architecture: Section 3.1 (API Gateway), Section 3.2 (REST API Server)
- API Spec: Section 1 (API Design Principles)
- Issue #017 (Exception Hierarchy), #018 (Logging), #019 (Config)

## Acceptance Criteria

- [ ] `backend/src/ground_control/main.py` — FastAPI app factory:
  - `create_app() → FastAPI` function (testable, configurable)
  - Lifespan handler: startup (DB pool, Redis, search), shutdown (cleanup)
  - Middleware stack (ordered):
    1. CORS middleware (configurable origins from settings)
    2. Request ID middleware (generates/extracts `X-Request-ID`)
    3. Tenant context middleware (extracts `X-Tenant-ID`)
    4. Authentication middleware (validates JWT/API key)
    5. Request logging middleware (log request/response)
    6. Rate limiting middleware
  - Global exception handlers (#017)
  - OpenAPI configuration (title, version, description, servers)
- [ ] `backend/src/ground_control/dependencies.py` — FastAPI dependencies:
  - `get_db` — async database session
  - `get_current_user` — extract authenticated user from token
  - `get_current_tenant` — extract tenant context
  - `get_settings` — application settings
- [ ] Health endpoints: `/health`, `/health/ready`, `/health/live`
- [ ] OpenAPI spec served at `/api/v1/openapi.json`
- [ ] Swagger UI at `/api/v1/docs` (disabled in production)
- [ ] `uvicorn` configuration for development and production
- [ ] Integration test: app starts, health check returns 200
