---
title: "Implement control catalog management service"
labels: [backend, control-management, domain-logic]
phase: 4
priority: P0
---

## Description

Build the domain service for control catalog management, including CRUD, framework mapping, CCL adoption, and effectiveness tracking.

## References

- User Stories: US-2.1 (Maintain Control Catalog), US-2.3 (Use CCL)
- PRD: Section 4.2 (Control Management)

## Acceptance Criteria

- [ ] `ControlService`:
  - CRUD with taxonomy validation
  - Framework mapping management (add, remove, list)
  - CCL adoption (creates tenant control linked to CCL entry, inherits mappings)
  - Effectiveness update based on latest test results
  - Version history (control changes tracked with diff)
- [ ] Auto-update effectiveness_rating when test procedures complete
- [ ] Domain events: `control.created`, `control.updated`, `control.effectiveness_changed`
- [ ] Integration tests for CCL adoption flow
