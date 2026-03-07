---
title: "Implement artifact/evidence API endpoints"
labels: [api, backend, evidence]
phase: 2
priority: P0
---

## Description

Implement the REST API for artifact management including upload (pre-signed URL flow), download, versioning, and linking.

## References

- API Spec: Section 4.6 (Artifacts — all endpoints, upload flow)
- User Stories: US-4.1 (Upload Artifacts), US-4.2 (Link Evidence), US-4.4 (Evidence Lineage)

## Acceptance Criteria

- [ ] Upload flow:
  - `POST /api/v1/artifacts/upload-url` → returns pre-signed S3 URL
  - Client uploads directly to S3
  - `POST /api/v1/artifacts` — registers artifact, verifies hash
- [ ] CRUD endpoints per API spec
- [ ] Versioning: `GET /{id}/versions`, `POST /{id}/versions`
- [ ] Linking: `GET /{id}/links`, `POST /{id}/links`, `DELETE /{id}/links/{link_id}`
- [ ] Lineage: `GET /{id}/lineage` — full chain of custody timeline
- [ ] Download: `GET /{id}/download` → redirect to pre-signed URL
- [ ] File size validation (configurable max, default 500MB)
- [ ] SHA-256 hash verification on registration
- [ ] Domain service: `EvidenceService`
- [ ] Integration tests with mocked S3
