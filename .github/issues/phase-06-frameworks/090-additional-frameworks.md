---
title: "Create PCI-DSS v4.0, CIS Controls v8, and supplemental framework definitions"
labels: [compliance, frameworks, content]
phase: 6
priority: P2
---

## Description

Create additional framework definitions for PCI-DSS, CIS Controls, COBIT, GDPR, and HIPAA.

## References

- PRD: Section 3 (PCI-DSS v4.0, CIS Controls v8, COBIT 2019, GDPR, HIPAA)

## Acceptance Criteria

- [ ] `plugins/frameworks/pci-dss-v4.yaml` — 12 requirements with sub-requirements
- [ ] `plugins/frameworks/cis-controls-v8.yaml` — safeguards mapped to implementation groups
- [ ] `plugins/frameworks/cobit-2019.yaml` — governance and management objectives
- [ ] `plugins/frameworks/gdpr-article32.yaml` — data protection controls overlay
- [ ] `plugins/frameworks/hipaa-security.yaml` — administrative, physical, technical safeguards
- [ ] All with CCL mappings where applicable
- [ ] All loaded via framework loader
