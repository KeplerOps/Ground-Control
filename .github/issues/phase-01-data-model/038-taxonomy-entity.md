---
title: "Implement taxonomy configuration entity"
labels: [data-model, backend, configuration]
phase: 1
priority: P1
---

## Description

Create the taxonomy configuration entity that stores configurable enumerations (risk categories, control types, likelihood/impact scales, rating scales, etc.) per tenant.

## References

- Data Model: Section 2.17 (Taxonomy Configuration)
- PRD: Section 6.1 (Shared Taxonomy)
- User Stories: US-7.5 (Manage Taxonomy & Configuration)

## Acceptance Criteria

- [ ] SQLAlchemy model: `TaxonomyCategory` with fields (taxonomy_type, value, label, description, color, sort_order, is_active)
- [ ] Alembic migration with unique constraint on (tenant_id, taxonomy_type, value)
- [ ] Seed migration with default taxonomy values:
  - Risk categories: Access Management, Change Management, Operations, Data Protection, Third Party, Business Continuity
  - Control types: Preventive, Detective, Corrective
  - Control nature: Manual, Automated, IT-Dependent Manual
  - Likelihood: 1-5 scale with labels (Rare, Unlikely, Possible, Likely, Almost Certain)
  - Impact: 1-5 scale with labels (Negligible, Minor, Moderate, Major, Severe)
  - Effectiveness: Effective, Needs Improvement, Ineffective
- [ ] Repository: `TaxonomyRepository` with CRUD, ordering, activation/deactivation
- [ ] Pydantic schemas with validation against active taxonomy values
- [ ] Cache-friendly: taxonomy rarely changes, should be cached (#019 Redis config)
- [ ] Unit tests
