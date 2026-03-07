---
title: "Implement remediation plan and actions entities"
labels: [data-model, backend, findings]
phase: 1
priority: P1
---

## Description

Create remediation plan and action entities for tracking finding resolution.

## References

- Data Model: Section 2.13 (Remediation Plan, Remediation Actions)
- User Stories: US-5.2 (Manage Remediation), US-5.3 (Validate Remediation)

## Acceptance Criteria

- [ ] SQLAlchemy models: `RemediationPlan`, `RemediationAction`
- [ ] Plan: finding_id, description, owner_id, target_date, status
- [ ] Action: plan_id, description, owner_id, due_date, status, sort_order
- [ ] Alembic migration
- [ ] Repository with CRUD and status management
- [ ] Pydantic schemas
- [ ] Status tracking: planned → in_progress → completed → validated
- [ ] Unit tests
