---
title: "Implement risk-control linkage"
labels: [backend, domain-logic]
phase: 4
priority: P1
---

## Description

Build the many-to-many relationship between risks and controls, enabling residual risk calculation based on control effectiveness.

## References

- Data Model: Section 3 (Risk *--* Control via risk_control_mappings)
- PRD: Section 4.1 (Controls linked to risks for residual scoring)

## Acceptance Criteria

- [ ] Junction table: `risk_control_mappings` (risk_id, control_id, mapping_notes)
- [ ] Alembic migration
- [ ] API endpoints to link/unlink controls from risks
- [ ] Residual risk recalculation when control effectiveness changes
- [ ] View: "controls mitigating this risk" and "risks this control mitigates"
- [ ] Unit tests
