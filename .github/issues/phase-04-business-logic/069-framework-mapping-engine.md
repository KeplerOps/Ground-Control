---
title: "Implement control-framework mapping engine and gap analysis"
labels: [backend, compliance, domain-logic]
phase: 4
priority: P0
---

## Description

Build the engine that manages cross-framework control mappings and performs gap analysis (framework requirements without mapped controls).

## References

- User Stories: US-2.2 (Map Controls Across Frameworks)
- Use Cases: UC-05 (Cross-Framework Control Mapping)
- PRD: Section 6.2 (CCL — "test once, comply many")

## Acceptance Criteria

- [ ] Mapping service:
  - Add/remove control-to-requirement mappings
  - Bulk mapping via CCL suggested mappings
  - Coverage matrix: which requirements are covered per framework
  - Gap analysis: requirements without any mapped controls
- [ ] Coverage percentage per framework
- [ ] Duplicate and circular mapping detection
- [ ] Agent suggestion integration point (stub for Phase 7)
- [ ] Contracts: no duplicate (control_id, requirement_id) pairs
- [ ] Unit tests including coverage calculation
