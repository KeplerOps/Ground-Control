---
title: "Define base Pydantic schemas and API response envelope"
labels: [foundation, backend, api]
phase: 0
priority: P0
---

## Description

Create the base Pydantic schema classes and API response envelope that all endpoints will use. This ensures uniform request/response formats across the entire API, matching the specification in the API design doc.

## References

- API Spec: Section 3 (Response Format — single resource, collection, error)
- API Spec: Section 1 (API Design Principles — consistent envelopes)

## Acceptance Criteria

- [ ] `backend/src/ground_control/schemas/base.py`:
  - `BaseSchema` — base class with `model_config` (camelCase aliases, from_attributes)
  - `TimestampMixin` — `created_at`, `updated_at` fields
  - `TenantScopedMixin` — `tenant_id` field
- [ ] `backend/src/ground_control/schemas/envelope.py`:
  - `SingleResponse[T]` — `{"data": T}` wrapper (generic)
  - `CollectionResponse[T]` — `{"data": [T], "meta": PaginationMeta, "links": PaginationLinks}`
  - `PaginationMeta` — `total`, `page`, `per_page`, `total_pages`
  - `PaginationLinks` — `self`, `next`, `prev`, `first`, `last`
  - `ErrorResponse` — `{"error": {"code": str, "message": str, "details": list, "request_id": str}}`
  - `ErrorDetail` — `{"field": str, "message": str, "code": str}`
- [ ] `backend/src/ground_control/schemas/pagination.py`:
  - `PaginationParams` — query parameter model (`page`, `per_page`, `sort`, `fields`)
  - Validation: `page >= 1`, `1 <= per_page <= 100`
- [ ] `backend/src/ground_control/schemas/filters.py`:
  - Base filter schema with operator support (`[gte]`, `[lte]`, `[in]`, `[contains]`)
- [ ] All schemas use strict Pydantic v2 mode
- [ ] CamelCase serialization for API output, snake_case internally
- [ ] Unit tests for serialization/deserialization of all base schemas

## Technical Notes

- Use Pydantic's `model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)`
- Generic types: `SingleResponse[RiskRead]` produces `{"data": {...risk fields...}}`
- The envelope pattern prevents JSON array root (security best practice)
- Consider using Pydantic's `computed_field` for pagination link generation
