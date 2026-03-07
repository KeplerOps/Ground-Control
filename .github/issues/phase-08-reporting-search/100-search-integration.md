---
title: "Implement search integration (Meilisearch) and indexing"
labels: [backend, search, infrastructure]
phase: 8
priority: P0
---

## Description

Integrate Meilisearch for full-text search across risks, controls, findings, artifacts, and framework requirements.

## References

- Architecture: Section 3.7 (Search Index — Meilisearch)
- Data Model: Section 4.3 (Search Index — indexed entities)
- API Spec: Section 4.14 (Search endpoint)

## Acceptance Criteria

- [ ] Meilisearch client setup in `backend/src/ground_control/infrastructure/search/`
- [ ] Index configuration for: risks, controls, findings, artifacts, framework_requirements
- [ ] Searchable fields per entity (title, description, ref_id, tags)
- [ ] Filterable attributes (tenant_id, status, category, type)
- [ ] Index sync: event-driven (on create/update/delete) + full rebuild command
- [ ] Search API: `GET /api/v1/search?q=...&type=...` with typo-tolerant results
- [ ] Tenant isolation in search (filter by tenant_id)
- [ ] `gc-admin reindex-search` CLI command for full rebuild
- [ ] Unit tests with embedded Meilisearch or mock
