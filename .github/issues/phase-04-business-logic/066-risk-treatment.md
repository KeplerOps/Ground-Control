---
title: "Implement risk treatment plan management"
labels: [backend, risk-management, domain-logic]
phase: 4
priority: P1
---

## Description

Build the service for managing risk treatment plans (accept, mitigate, transfer, avoid) with linked action items.

## References

- User Stories: US-1.4 (Track Risk Treatment Plans)
- Data Model: Section 2.16 (Risk Treatment, Treatment Actions)

## Acceptance Criteria

- [ ] Treatment plan CRUD linked to risks
- [ ] Treatment types: accept, mitigate, transfer, avoid
- [ ] Action items with owners, due dates, status tracking
- [ ] Completion of all actions triggers residual risk re-assessment prompt
- [ ] Overdue action notifications
- [ ] Treatment plan history is auditable
- [ ] Domain events: `treatment.created`, `treatment.completed`
- [ ] Unit tests
