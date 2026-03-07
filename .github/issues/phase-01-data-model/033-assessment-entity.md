---
title: "Implement assessment campaign entity"
labels: [data-model, backend, assessment]
phase: 1
priority: P0
---

## Description

Create the assessment campaign entity. Campaigns are time-boxed cycles for evaluating controls (e.g., "Q1 2026 SOX ITGC Testing").

## References

- Data Model: Section 2.8 (Assessment Campaign)
- PRD: Section 4.3 (Assessment & Testing)
- User Stories: US-3.1 (Plan Assessment Campaign)
- Use Cases: UC-03 (Execute Test Procedures)

## Acceptance Criteria

- [ ] SQLAlchemy model: `AssessmentCampaign` with all fields (name, campaign_type, status, period dates, scope_filter, etc.)
- [ ] Status enum: planning, active, review, finalized, archived
- [ ] Alembic migration
- [ ] Repository: `AssessmentCampaignRepository` with CRUD + status transitions
- [ ] Pydantic schemas: `CampaignCreate`, `CampaignRead`, `CampaignUpdate`, `CampaignProgress`
- [ ] `scope_filter` (JSONB) defines which controls/systems are in scope
- [ ] Status transition validation (e.g., can't go from `planning` to `finalized`)
- [ ] Contracts: `period_end >= period_start`, valid status transitions
- [ ] Unit tests
