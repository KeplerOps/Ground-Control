---
title: "Implement control API endpoints"
labels: [api, backend, control-management]
phase: 2
priority: P0
---

## Description

Implement the REST API for control management, including framework mapping sub-endpoints and AI suggestion endpoint.

## References

- API Spec: Section 4.2 (Controls — all endpoints)
- User Stories: US-2.1, US-2.2 (Maintain Control Catalog, Map Controls)
- Issue #030 (Control Entity), #031 (Framework Entity)

## Acceptance Criteria

- [ ] Endpoints per API spec:
  - CRUD: `GET/POST/PUT/PATCH/DELETE /api/v1/controls[/{id}]`
  - `GET /api/v1/controls/{id}/mappings` — framework mappings
  - `POST /api/v1/controls/{id}/mappings` — add mapping
  - `DELETE /api/v1/controls/{id}/mappings/{mapping_id}` — remove
  - `POST /api/v1/controls/{id}/suggest-mappings` — AI suggestion (stub, implemented in Phase 7)
  - `GET /api/v1/controls/{id}/test-history` — historical test results
- [ ] Control domain service: `ControlService`
- [ ] All mutations audit-logged
- [ ] Response envelope format
- [ ] Integration tests
