---
title: "Implement risk API endpoints"
labels: [api, backend, risk-management]
phase: 2
priority: P0
---

## Description

Implement the full REST API for risk management, including CRUD, filtering, pagination, and sub-resource endpoints.

## References

- API Spec: Section 4.1 (Risks — all endpoints)
- User Stories: US-1.1 (Maintain Risk Register)
- Use Cases: UC-01 (Manage Risk Register)
- Issue #029 (Risk Entity)

## Acceptance Criteria

- [ ] Endpoints per API spec:
  - `GET /api/v1/risks` — list with filtering, pagination, sorting, field selection, includes
  - `POST /api/v1/risks` — create (validates against taxonomy)
  - `GET /api/v1/risks/{id}` — get by ID with optional includes
  - `PUT /api/v1/risks/{id}` — full replace
  - `PATCH /api/v1/risks/{id}` — partial update
  - `DELETE /api/v1/risks/{id}` — archive (soft delete)
  - `GET /api/v1/risks/{id}/treatments` — list treatment plans
  - `POST /api/v1/risks/{id}/treatments` — create treatment plan
  - `GET /api/v1/risks/{id}/controls` — linked controls
  - `GET /api/v1/risks/{id}/artifacts` — linked evidence
  - `GET /api/v1/risks/{id}/audit-log` — audit history
- [ ] Risk domain service: `RiskService` orchestrates repository + audit logging + events
- [ ] All mutations logged to audit trail
- [ ] Response format matches envelope spec (#020)
- [ ] Filter parameters per API spec (category, status, score ranges, owner, etc.)
- [ ] Authorization checked per endpoint (uses dependencies from #041)
- [ ] Integration tests with test database
- [ ] Input validation produces 422 with field-level errors
