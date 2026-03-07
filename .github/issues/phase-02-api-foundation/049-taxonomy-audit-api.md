---
title: "Implement taxonomy, audit log, and search API endpoints"
labels: [api, backend, configuration, audit]
phase: 2
priority: P1
---

## Description

Implement the REST API for taxonomy configuration, audit log querying, and full-text search.

## References

- API Spec: Section 4.11 (Taxonomy), 4.13 (Audit Logs), 4.14 (Search)
- User Stories: US-7.4 (View Audit Logs), US-7.5 (Manage Taxonomy)

## Acceptance Criteria

- [ ] Taxonomy: list types, list values, add value, update value
- [ ] Audit logs: filterable query (date range, actor, action, entity), export (CSV/JSON)
- [ ] Audit log queries are read-only (no mutation endpoints)
- [ ] Search: `GET /api/v1/search?q=...&type=...` — full-text across entities
- [ ] Search delegates to Meilisearch (stub if not yet integrated)
- [ ] Integration tests
