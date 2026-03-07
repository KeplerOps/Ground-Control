---
title: "Implement evidence request API endpoints"
labels: [api, backend, evidence]
phase: 2
priority: P1
---

## Description

Implement the REST API for evidence requests — the workflow where auditors request evidence from control owners.

## References

- API Spec: Section 4.7 (Evidence Requests)
- User Stories: US-3.3 (Collect Evidence via Requests)
- Use Cases: UC-04 (Collect Evidence)

## Acceptance Criteria

- [ ] Endpoints: list, create, update, submit, accept, reject
- [ ] Submit endpoint allows control owner to upload evidence and link it
- [ ] Accept/reject by auditor with comments
- [ ] Overdue detection (status auto-updated based on due_date)
- [ ] Domain service: `EvidenceRequestService`
- [ ] Integration tests
