---
title: "Implement finding entity"
labels: [data-model, backend, findings]
phase: 1
priority: P0
---

## Description

Create the finding entity for documenting control deficiencies identified during assessments.

## References

- Data Model: Section 2.12 (Finding)
- PRD: Section 4.5 (Findings & Issues)
- User Stories: US-5.1 (Record Findings)

## Acceptance Criteria

- [ ] SQLAlchemy model: `Finding` with all fields (ref_id, campaign_id, control_id, procedure_id, title, description, root_cause, risk_rating, classification, status, owner_id, due_date, agent_produced)
- [ ] Status enum: draft, open, remediation_in_progress, validation, closed
- [ ] Classification enum: deficiency, significant_deficiency, material_weakness
- [ ] Alembic migration
- [ ] Repository: `FindingRepository` with CRUD, status transitions, filtering
- [ ] Pydantic schemas: `FindingCreate`, `FindingRead`, `FindingUpdate`, `FindingFilter`
- [ ] `ref_id` auto-generation: `FIND-{sequence}`
- [ ] Status transition contracts (e.g., can't close without going through validation)
- [ ] Unit tests
