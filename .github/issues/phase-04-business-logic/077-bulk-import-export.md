---
title: "Implement bulk import/export (CSV, JSON)"
labels: [backend, domain-logic, data-migration]
phase: 4
priority: P1
---

## Description

Build bulk import and export capabilities for risks, controls, and findings from CSV and JSON formats.

## References

- User Stories: US-1.1 (Bulk import risks from CSV or JSON)
- Use Cases: UC-01 (extension 3a — CSV/JSON import)

## Acceptance Criteria

- [ ] Import service for risks, controls, findings:
  - Parse CSV/JSON with configurable field mapping
  - Validate each row against schemas and taxonomy
  - Report row-level errors (don't fail entire import on one bad row)
  - Return summary: imported, skipped, errors
- [ ] Export service: query → CSV, JSON, Excel formats
- [ ] API endpoints: `POST /api/v1/{entity}/import`, `GET /api/v1/{entity}/export`
- [ ] Import is transactional: all-or-nothing option or partial-commit option
- [ ] Background processing for large imports (> 1000 rows)
- [ ] Import templates downloadable (CSV with headers)
- [ ] Unit tests with sample data files
