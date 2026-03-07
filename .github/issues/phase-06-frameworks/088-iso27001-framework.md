---
title: "Create ISO 27001:2022 framework definition"
labels: [compliance, frameworks, content]
phase: 6
priority: P1
---

## Description

Create the ISO 27001:2022 framework with Annex A controls and Statement of Applicability support.

## References

- PRD: Section 3 (ISO 27001:2022 — Full, Annex A controls, SoA generation)

## Acceptance Criteria

- [ ] `plugins/frameworks/iso27001-2022.yaml` with all Annex A controls (93 controls, 4 themes)
- [ ] Themes: Organizational, People, Physical, Technological
- [ ] CCL mappings for cross-framework coverage
- [ ] Statement of Applicability (SoA) generation support
- [ ] Loaded via framework loader
