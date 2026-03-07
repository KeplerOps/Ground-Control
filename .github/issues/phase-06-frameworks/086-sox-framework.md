---
title: "Create SOX ITGC framework definition"
labels: [compliance, frameworks, content]
phase: 6
priority: P0
---

## Description

Create the SOX IT General Controls framework definition with control objectives, test procedures, and CCL mappings.

## References

- PRD: Section 3 (SOX ITGC — Full coverage)
- PRD: Section 4.3 (Assessment — SOX ITGC testing)

## Acceptance Criteria

- [ ] `plugins/frameworks/sox-itgc.yaml` with:
  - SOX ITGC domains: Access to Programs and Data, Program Changes, Program Development, Computer Operations
  - Control objectives per domain
  - Sub-objectives and testing guidance
  - CCL mappings for each objective
- [ ] Walkthroughs guidance for each control area
- [ ] Deficiency classification guidance (deficiency → significant deficiency → material weakness)
- [ ] Loaded via framework loader (#085)
- [ ] Verified against authoritative SOX ITGC references
