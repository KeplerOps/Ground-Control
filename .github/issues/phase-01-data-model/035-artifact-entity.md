---
title: "Implement artifact (evidence) entity and storage abstraction"
labels: [data-model, backend, evidence]
phase: 1
priority: P0
---

## Description

Create the artifact entity, artifact linking (polymorphic many-to-many), and the storage abstraction layer for S3-compatible object storage.

## References

- Data Model: Section 2.10 (Artifact), artifact_links
- PRD: Section 4.4 (Evidence & Artifact Management)
- User Stories: US-4.1 (Upload and Manage Artifacts), US-4.2 (Link Evidence)
- Architecture: Section 3.7 (Object Store — S3/MinIO)

## Acceptance Criteria

- [ ] SQLAlchemy models: `Artifact`, `ArtifactLink`
- [ ] Artifact: filename, content_type, size_bytes, storage_key, sha256_hash, version, encryption fields, retention
- [ ] ArtifactLink: polymorphic link (artifact_id, entity_type, entity_id, context_note)
- [ ] Alembic migration with indexes on hash and entity links
- [ ] Storage abstraction: `backend/src/ground_control/infrastructure/storage/`:
  - `interface.py` — `ObjectStorage` protocol (upload, download, delete, presigned_url)
  - `s3.py` — S3/MinIO implementation using `boto3`
  - `local.py` — local filesystem implementation (development fallback)
- [ ] Repository: `ArtifactRepository` with CRUD, versioning, linking
- [ ] Pydantic schemas: `ArtifactCreate`, `ArtifactRead`, `ArtifactLinkCreate`
- [ ] SHA-256 hash verification on upload
- [ ] Version tracking: new upload creates version, previous retained
- [ ] Unit tests with mocked storage
