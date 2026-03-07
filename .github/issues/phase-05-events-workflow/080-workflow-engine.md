---
title: "Implement workflow engine (configurable state machines)"
labels: [backend, domain-logic, workflow]
phase: 5
priority: P0
---

## Description

Build a generic, configurable workflow engine for review and approval chains. Used for workpaper review, finding validation, evidence acceptance, and custom workflows.

## References

- PRD: Section 4.7 (Review Workflows — multi-level chains)
- User Stories: US-3.4 (Review and Approve Workpapers)

## Acceptance Criteria

- [ ] `WorkflowEngine`:
  - Configurable review chains: preparer → reviewer → approver
  - Multi-level chains (1, 2, or 3 levels, configurable per entity type)
  - Transition actions: approve, reject (with comments), request_changes
  - Automatic routing to next reviewer on approval
  - Lock entity on final approval (prevent further edits)
- [ ] Workflow configuration per tenant and entity type
- [ ] Review status tracking on campaigns dashboard
- [ ] Rejection returns to preparer with reviewer comments
- [ ] Domain events: `workflow.submitted`, `workflow.approved`, `workflow.rejected`
- [ ] Unit tests for all workflow paths
