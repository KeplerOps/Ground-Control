---
title: "Implement finding and remediation API endpoints"
labels: [api, backend, findings]
phase: 2
priority: P0
---

## Description

Implement the REST API for findings, remediation plans, and remediation validation.

## References

- API Spec: Section 4.8 (Findings — all endpoints)
- User Stories: US-5.1, US-5.2, US-5.3 (Record Findings, Manage Remediation, Validate)

## Acceptance Criteria

- [ ] Finding endpoints: CRUD, remediation sub-resources, validate, close
- [ ] Remediation plan: create, update, list actions
- [ ] Finding closure requires validation step (enforced by state machine)
- [ ] Domain service: `FindingService`
- [ ] All mutations audit-logged
- [ ] Integration tests
