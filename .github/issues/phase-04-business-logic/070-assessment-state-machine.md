---
title: "Implement assessment campaign state machine"
labels: [backend, assessment, domain-logic]
phase: 4
priority: P0
---

## Description

Build a generic, configurable state machine for assessment campaigns and test procedures. State transitions are validated, logged, and can trigger domain events.

## References

- PRD: Section 4.3 (Assessment Campaigns — lifecycle states)
- Data Model: Section 2.8 (status enum), Section 2.9 (test procedure status)
- Issue #023 (Design-by-Contract — state machine contracts)

## Acceptance Criteria

- [ ] Generic `StateMachine` class:
  - Configurable states and valid transitions
  - Transition guards (callable conditions)
  - Transition actions (side effects on transition)
  - Audit logging of every transition
- [ ] Campaign state machine: planning → active → review → finalized → archived
- [ ] Test procedure state machine: not_started → in_progress → completed → review → approved
- [ ] Finding state machine: draft → open → remediation_in_progress → validation → closed
- [ ] Contracts (formally verified):
  - `@icontract.require(lambda new_state: new_state in VALID_TRANSITIONS[current_state])`
  - No state can transition to itself (unless explicitly allowed)
  - Terminal states (archived, closed) cannot transition further
- [ ] CrossHair verification of state machine invariants
- [ ] Unit tests for all valid and invalid transitions
