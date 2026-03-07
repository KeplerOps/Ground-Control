---
title: "Implement Common Control Library (CCL) entity"
labels: [data-model, backend, compliance]
phase: 1
priority: P1
---

## Description

Create the CCL entity and CCL-to-framework-requirement mapping. The CCL provides reusable, standardized control definitions that map across multiple frameworks.

## References

- Data Model: Section 2.7 (Common Control Library)
- PRD: Section 6.2 (Common Control Library)
- User Stories: US-2.3 (Use Common Control Library)

## Acceptance Criteria

- [ ] SQLAlchemy models: `CCLEntry`, `CCLFrameworkMapping`
- [ ] Alembic migration
- [ ] CCL entries are global (not tenant-scoped) — shared across all tenants
- [ ] `ref_id` pattern: `CC-{category}-{sequence}` (e.g., `CC-AM-001`)
- [ ] Repository: `CCLRepository` with browse, search, and adopt operations
- [ ] Pydantic schemas: `CCLEntryRead`, `CCLEntryCreate`, `CCLAdoptRequest`
- [ ] "Adopt" operation: creates a tenant-scoped Control linked to the CCL entry
- [ ] Unit tests
