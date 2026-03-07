---
title: "Implement risk entity (model, schema, repository)"
labels: [data-model, backend, risk-management]
phase: 1
priority: P0
---

## Description

Create the risk entity — the centerpiece of the risk management domain. Includes the SQLAlchemy model, Pydantic schemas, and repository with full CRUD and filtering support.

## References

- Data Model: Section 2.4 (Risk)
- PRD: Section 4.1 (Risk Management)
- User Stories: US-1.1 (Maintain Risk Register), US-1.2, US-1.3, US-1.4, US-1.5
- Use Cases: UC-01 (Manage Risk Register), UC-02 (Run Risk Assessment)

## Acceptance Criteria

- [ ] SQLAlchemy model: `Risk` with all fields from data model (ref_id, title, description, category, owner_id, inherent/residual scores, appetite, business_units, tags, custom_fields, etc.)
- [ ] Computed columns: `inherent_score = inherent_likelihood * inherent_impact` (STORED)
- [ ] Alembic migration with indexes (tenant_category, tenant_owner, tenant_status)
- [ ] Repository: `RiskRepository` with:
  - `create`, `get_by_id`, `get_by_ref_id`, `list` (with filters), `update`, `archive`
  - Filtering: category, status, owner_id, score ranges, business_unit, tags
  - Sorting: any field, default `-inherent_score`
- [ ] Pydantic schemas:
  - `RiskCreate` — validates likelihood/impact ranges (1-5 by default, configurable)
  - `RiskRead` — includes computed scores, relationships
  - `RiskUpdate` — partial update
  - `RiskSummary` — lightweight for lists
  - `RiskFilter` — filter parameters
- [ ] `ref_id` auto-generation: `RISK-{sequence}` if not provided
- [ ] Soft delete: `archive()` sets `status='archived'` and `archived_at`
- [ ] Contracts:
  - `@icontract.require(lambda likelihood: 1 <= likelihood <= 5)`
  - `@icontract.require(lambda impact: 1 <= impact <= 5)`
  - `@icontract.ensure(lambda result: result.inherent_score == result.inherent_likelihood * result.inherent_impact)`
- [ ] Unit tests for model, schema validation, repository methods

## Technical Notes

- `custom_fields` (JSONB) allows tenant-specific fields without schema changes
- `business_units` and `tags` are `ARRAY(TEXT)` for flexible categorization
- Generated columns (`inherent_score`, `residual_score`) are computed by PostgreSQL — read-only in ORM
