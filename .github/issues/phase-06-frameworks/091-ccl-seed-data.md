---
title: "Create Common Control Library seed data"
labels: [compliance, content, data]
phase: 6
priority: P0
---

## Description

Create the initial seed data for the Common Control Library with standardized control definitions mapped across all supported frameworks.

## References

- PRD: Section 6.2 (Common Control Library — CCL is the heart of reusability)
- Data Model: Section 2.7 (CCL Entries)

## Acceptance Criteria

- [ ] CCL seed data covering major control domains:
  - Access Management (CC-AM-001 through CC-AM-010)
  - Change Management (CC-CM-001 through CC-CM-008)
  - Operations (CC-OP-001 through CC-OP-008)
  - Data Protection (CC-DP-001 through CC-DP-006)
  - Incident Management (CC-IM-001 through CC-IM-005)
  - Business Continuity (CC-BC-001 through CC-BC-004)
- [ ] Each entry includes: ref_id, title, description, category, control_type, control_nature
- [ ] Cross-framework mappings for each entry (SOX, SOC 2, ISO 27001, NIST at minimum)
- [ ] Seed data loadable via CLI: `gc-admin seed-ccl`
- [ ] Data format: YAML for human review, loaded programmatically
