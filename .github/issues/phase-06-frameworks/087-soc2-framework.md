---
title: "Create SOC 2 Trust Services framework definition"
labels: [compliance, frameworks, content]
phase: 6
priority: P0
---

## Description

Create the SOC 2 Trust Services Criteria framework definition covering all five categories.

## References

- PRD: Section 3 (SOC 2 — Full, all five categories)

## Acceptance Criteria

- [ ] `plugins/frameworks/soc2-tsc.yaml` with:
  - Categories: Security (CC), Availability (A), Processing Integrity (PI), Confidentiality (C), Privacy (P)
  - All Common Criteria (CC1-CC9) with points of focus
  - Additional criteria per category
  - CCL mappings
- [ ] Loaded via framework loader
- [ ] Coverage matrix generation verified
