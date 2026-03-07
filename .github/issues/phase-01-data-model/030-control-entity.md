---
title: "Implement control entity (model, schema, repository)"
labels: [data-model, backend, control-management]
phase: 1
priority: P0
---

## Description

Create the control entity for the control catalog. Controls are reusable definitions linked to the Common Control Library and mapped to framework requirements.

## References

- Data Model: Section 2.5 (Control)
- PRD: Section 4.2 (Control Management)
- User Stories: US-2.1 (Maintain Control Catalog), US-2.2, US-2.3

## Acceptance Criteria

- [ ] SQLAlchemy model: `Control` with all fields (ref_id, ccl_entry_id, title, objective, description, control_type, control_nature, frequency, owner_id, effectiveness_rating, etc.)
- [ ] Alembic migration with indexes
- [ ] Enum validation: control_type ∈ {preventive, detective, corrective}, control_nature ∈ {manual, automated, it_dependent_manual}, frequency ∈ {continuous, daily, weekly, monthly, quarterly, annual, ad_hoc}
- [ ] Repository: `ControlRepository` with CRUD + filtering by framework, type, nature, owner, effectiveness
- [ ] Pydantic schemas: `ControlCreate`, `ControlRead`, `ControlUpdate`, `ControlSummary`, `ControlFilter`
- [ ] `ref_id` pattern: `CTRL-{category_abbrev}-{sequence}` (e.g., `CTRL-AM-001`)
- [ ] Foreign key to `ccl_entries` (optional — not all controls come from CCL)
- [ ] Relationship to owner (User)
- [ ] Unit tests for all repository methods
- [ ] Contracts: control_type and control_nature must be valid enum values
