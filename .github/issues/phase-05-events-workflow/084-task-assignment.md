---
title: "Implement task assignment and SLA tracking"
labels: [backend, collaboration, domain-logic]
phase: 5
priority: P1
---

## Description

Build the task assignment system with due dates and SLA tracking for all assignable work items (test procedures, evidence requests, remediation actions).

## References

- PRD: Section 4.7 (Task Assignment — due dates, SLA tracking)

## Acceptance Criteria

- [ ] Generic task assignment model (or convention across entities):
  - Assigned_to (user or group), due_date, priority, status
  - SLA configuration per task type (e.g., evidence requests: 5 business days)
- [ ] Task list API: `GET /api/v1/users/{id}/tasks` — all assigned items across entity types
- [ ] Overdue detection and escalation
- [ ] SLA breach notifications (approaching and breached)
- [ ] Dashboard data: tasks by status, overdue counts, SLA compliance percentage
- [ ] Unit tests
