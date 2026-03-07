---
title: "Create NIST CSF 2.0 and SP 800-53 Rev 5 framework definitions"
labels: [compliance, frameworks, content]
phase: 6
priority: P1
---

## Description

Create NIST Cybersecurity Framework 2.0 and NIST SP 800-53 Rev 5 framework definitions.

## References

- PRD: Section 3 (NIST CSF 2.0 — Full, NIST 800-53 — Full with enhancements)

## Acceptance Criteria

- [ ] `plugins/frameworks/nist-csf-2.0.yaml`:
  - Six functions: Govern, Identify, Protect, Detect, Respond, Recover
  - Categories and subcategories
  - CCL mappings
- [ ] `plugins/frameworks/nist-800-53-r5.yaml`:
  - All 20 control families
  - Base controls and enhancements
  - CCL mappings
- [ ] Both loaded via framework loader
