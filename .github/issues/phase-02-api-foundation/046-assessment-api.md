---
title: "Implement assessment and test procedure API endpoints"
labels: [api, backend, assessment]
phase: 2
priority: P0
---

## Description

Implement the REST API for assessment campaigns, test procedures, and test steps.

## References

- API Spec: Section 4.4 (Assessment Campaigns), Section 4.5 (Test Procedures)
- User Stories: US-3.1, US-3.2, US-3.4 (Plan Campaign, Execute Tests, Review Workpapers)
- Use Cases: UC-03 (Execute Test Procedures)

## Acceptance Criteria

- [ ] Assessment campaign endpoints: CRUD, finalize, progress, test-procedures listing
- [ ] Test procedure endpoints: get, update, submit results, steps CRUD, submit-for-review, approve, reject
- [ ] Campaign finalization locks all child test procedures
- [ ] Progress endpoint returns completion percentage, overdue counts
- [ ] Result submission accepts both human and agent payloads
- [ ] Domain services: `AssessmentService`, `TestProcedureService`
- [ ] All mutations audit-logged
- [ ] Integration tests
