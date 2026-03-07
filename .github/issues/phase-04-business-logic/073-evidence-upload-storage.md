---
title: "Implement evidence upload and S3 storage integration"
labels: [backend, evidence, infrastructure]
phase: 4
priority: P0
---

## Description

Build the complete evidence upload flow: pre-signed URL generation, S3 storage, hash verification, and metadata registration.

## References

- API Spec: Section 4.6 (Upload flow — pre-signed URL pattern)
- PRD: Section 4.4 (Artifact Store — encryption, hashing)
- Issue #035 (Artifact Entity)

## Acceptance Criteria

- [ ] Pre-signed URL generation for direct S3 upload (bypass API server)
- [ ] Storage key pattern: `{tenant_id}/{year}/{month}/{artifact_id}/{version}/{filename}`
- [ ] SHA-256 hash verification: client-provided hash compared to S3 object hash
- [ ] Server-side encryption: AES-256 (SSE-S3 or SSE-KMS)
- [ ] File size enforcement (configurable per tenant, default 500MB)
- [ ] Content-type validation (configurable allowed types)
- [ ] Virus scanning integration point (stub for future ClamAV plugin)
- [ ] Download via pre-signed URL (time-limited, 15 min default)
- [ ] Integration tests with MinIO (local S3-compatible)
