---
title: "Set up API versioning and router structure"
labels: [api, backend, architecture]
phase: 2
priority: P0
---

## Description

Establish the API versioning scheme and router organization. All endpoints live under `/api/v1/` with a clean, modular router structure.

## References

- API Spec: Section 4 (Core API Endpoints — all resource paths)
- API Spec: Section 10 (API Versioning & Deprecation)

## Acceptance Criteria

- [ ] Router hierarchy:
  ```
  /api/v1/
  ├── /auth/          → auth_router
  ├── /risks/         → risk_router
  ├── /controls/      → control_router
  ├── /frameworks/    → framework_router
  ├── /assessments/   → assessment_router
  ├── /test-procedures/ → test_procedure_router
  ├── /artifacts/     → artifact_router
  ├── /evidence-requests/ → evidence_request_router
  ├── /findings/      → finding_router
  ├── /agents/        → agent_router
  ├── /ccl/           → ccl_router
  ├── /taxonomy/      → taxonomy_router
  ├── /reports/       → report_router
  ├── /audit-logs/    → audit_log_router
  ├── /search/        → search_router
  ├── /webhooks/      → webhook_router
  └── /users/         → user_router
  ```
- [ ] Each router in its own module: `backend/src/ground_control/api/v1/{resource}.py`
- [ ] Version prefix applied at mount time, not repeated in each route
- [ ] Deprecation middleware: `Sunset` header on deprecated endpoints
- [ ] OpenAPI tags match router structure for organized Swagger docs
- [ ] `__init__.py` in api/v1 that assembles all routers
- [ ] Placeholder routers (empty, returning 501) for endpoints not yet implemented
