---
title: "Implement test procedure execution engine"
labels: [backend, assessment, domain-logic]
phase: 4
priority: P0
---

## Description

Build the service that manages test procedure execution — recording step results, calculating progress, and routing completed procedures for review.

## References

- User Stories: US-3.2 (Execute Test Procedures)
- Use Cases: UC-03 (Execute Test Procedures)

## Acceptance Criteria

- [ ] `TestExecutionService`:
  - Record step result (actual_result, conclusion, evidence_ids, notes)
  - Calculate procedure progress (% steps completed)
  - Mark procedure complete (validates all steps have conclusions)
  - Submit for review (transitions state, notifies reviewer)
  - Roll up to campaign progress
- [ ] Step conclusion validation: pass, fail, na
- [ ] Overall procedure conclusion derived from step results (any fail → ineffective)
- [ ] Evidence linking for individual steps
- [ ] Agent result submission (structured payload → step results)
- [ ] Domain events: `test.step_completed`, `test.procedure_completed`, `test.submitted_for_review`
- [ ] Integration tests
