---
title: "Implement risk assessment campaign workflow"
labels: [backend, risk-management, domain-logic]
phase: 4
priority: P0
---

## Description

Build the business logic for running risk assessment campaigns: creation, assignment, scoring, review, and finalization.

## References

- User Stories: US-1.2 (Conduct Risk Assessment Campaign)
- Use Cases: UC-02 (Run Risk Assessment Campaign)

## Acceptance Criteria

- [ ] Campaign lifecycle: `create → populate_risks → assign → assess → review → finalize`
- [ ] Scope-based risk population (filter by category, BU, status)
- [ ] Assignment of individual risks to assessors with notifications
- [ ] Assessor updates: likelihood, impact, justification, evidence links
- [ ] Campaign progress tracking (% complete, overdue items)
- [ ] Finalization locks all assessments, generates comparison report
- [ ] Cannot finalize with incomplete assessments (validation check)
- [ ] Prior period comparison (current vs previous campaign scores)
- [ ] Domain events: `campaign.created`, `campaign.finalized`, `assessment.updated`
- [ ] Integration tests for full campaign lifecycle
