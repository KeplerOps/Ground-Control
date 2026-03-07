---
title: "Build risk management views"
labels: [frontend, ui, risk-management]
phase: 9
priority: P0
---

## Description

Build the frontend views for risk management: risk register, risk detail, heat map, treatment plans.

## References

- User Stories: US-1.1-US-1.5 (all risk management stories)
- Use Cases: UC-01, UC-02

## Acceptance Criteria

- [ ] Risk register page:
  - Table view with columns: ref_id, title, category, inherent score, residual score, owner, status
  - Filtering, sorting, pagination
  - Quick actions: edit, archive
  - Create risk dialog/page
- [ ] Risk detail page:
  - All risk fields (editable)
  - Linked controls, evidence, treatment plans (tabs)
  - Audit history timeline
  - Risk score visualization (before/after controls)
- [ ] Risk heat map:
  - Interactive 5x5 matrix (or configurable)
  - Click cell to see risks at that position
  - Appetite threshold overlay
  - Export as image
- [ ] Risk assessment campaign view:
  - Campaign progress dashboard
  - Assigned risks list for assessors
  - Score update form with justification
