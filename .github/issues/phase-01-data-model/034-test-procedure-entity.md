---
title: "Implement test procedure and test steps entities"
labels: [data-model, backend, assessment]
phase: 1
priority: P0
---

## Description

Create the test procedure and test step entities. Test procedures belong to assessment campaigns and link to controls. Steps are the ordered test actions within a procedure.

## References

- Data Model: Section 2.9 (Test Procedure & Steps)
- PRD: Section 4.3 (Test Procedures, Workpapers, Sampling)
- User Stories: US-3.2 (Execute Test Procedures), US-3.5 (Agent-Performed Testing)
- Use Cases: UC-03 (Execute Test Procedures), UC-06 (Agent-Performed Testing)

## Acceptance Criteria

- [ ] SQLAlchemy models: `TestProcedure`, `TestStep`
- [ ] TestProcedure: campaign_id, control_id, status, assigned_to, reviewer_id, conclusion, agent fields, sampling fields
- [ ] TestStep: procedure_id, step_number, instruction, expected_result, actual_result, conclusion, notes
- [ ] Alembic migration with unique constraint on (procedure_id, step_number)
- [ ] Repositories for both entities
- [ ] Pydantic schemas including agent provenance fields
- [ ] Status transitions: not_started → in_progress → completed → review → approved
- [ ] Step conclusion enum: pass, fail, na
- [ ] Contracts: step_number must be positive, valid status transitions
- [ ] Unit tests
