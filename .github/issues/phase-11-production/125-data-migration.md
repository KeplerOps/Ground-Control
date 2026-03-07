---
title: "Build data migration tools (AuditBoard import)"
labels: [backend, data-migration, production]
phase: 11
priority: P2
---

## Description

Build import adapters for migrating data from AuditBoard and other GRC tools.

## References

- PRD: Section 11 (Risks & Mitigations — data migration from AuditBoard)

## Acceptance Criteria

- [ ] AuditBoard CSV/API export adapter:
  - Parse AuditBoard export formats (risks, controls, findings, evidence metadata)
  - Map AuditBoard fields to Ground Control schema
  - Handle differences in taxonomy and classification
- [ ] Generic import pipeline: CSV → validate → transform → load
- [ ] Migration report: items imported, skipped, errors, mapping decisions
- [ ] Dry-run mode (validate without committing)
- [ ] CLI: `gc-admin migrate --source auditboard --file export.csv`
- [ ] Unit tests with sample export data
