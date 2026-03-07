---
title: "Implement finding lifecycle and deficiency classification"
labels: [backend, findings, domain-logic]
phase: 4
priority: P0
---

## Description

Build the complete finding lifecycle management including deficiency classification, remediation tracking, and validation.

## References

- User Stories: US-5.1, US-5.2, US-5.3 (Record, Remediate, Validate Findings)
- PRD: Section 4.5 (Findings & Issues)

## Acceptance Criteria

- [ ] `FindingService`:
  - Create finding linked to control, procedure, campaign
  - Deficiency classification: deficiency, significant_deficiency, material_weakness
  - Lifecycle: draft → open → remediation_in_progress → validation → closed
  - Remediation plan creation and tracking
  - Validation testing (re-test after remediation)
  - Closure with evidence of resolution
- [ ] Duplicate finding detection (suggest potential matches by control + description similarity)
- [ ] Finding aggregation across campaigns (unified issues view)
- [ ] Closed findings update control effectiveness rating
- [ ] Domain events: `finding.opened`, `finding.closed`, `finding.overdue`
- [ ] Integration tests for full lifecycle
