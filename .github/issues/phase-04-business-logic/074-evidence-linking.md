---
title: "Implement evidence linking and evidence requests"
labels: [backend, evidence, domain-logic]
phase: 4
priority: P0
---

## Description

Build the polymorphic evidence linking system and evidence request lifecycle.

## References

- User Stories: US-4.2 (Link Evidence), US-3.3 (Collect Evidence via Requests)
- Use Cases: UC-04 (Collect Evidence)
- Data Model: Section 2.10 (artifact_links), Section 2.11 (evidence_requests)

## Acceptance Criteria

- [ ] Polymorphic linking: attach artifact to any entity (risk, control, test_step, finding)
- [ ] Context notes on links (why this evidence is relevant)
- [ ] Unlinking preserves audit trail
- [ ] Evidence request lifecycle: pending → submitted → accepted/rejected → overdue
- [ ] Overdue detection (background job or query-time calculation)
- [ ] Submission auto-links artifacts to the request's control/procedure
- [ ] Escalation notifications for overdue requests
- [ ] Domain events: `evidence.linked`, `evidence_request.submitted`, `evidence_request.overdue`
- [ ] Unit tests
