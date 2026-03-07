---
title: "Implement API key authentication"
labels: [api, backend, auth]
phase: 2
priority: P1
---

## Description

Implement API key-based authentication for service accounts and simple integrations. API keys are long-lived and provide an alternative to OAuth2 for scripts and CI tools.

## References

- API Spec: Section 2.3 (API Keys)
- PRD: Section 8.1 (Authentication — API Keys)

## Acceptance Criteria

- [ ] API key format: `gc_live_` prefix + 32 random bytes (base64)
- [ ] Key storage: Argon2id hash in database (never store plaintext)
- [ ] `Authorization: ApiKey <key>` header support
- [ ] Key management endpoints (admin):
  - `POST /api/v1/auth/api-keys` — create (returns key once, hash stored)
  - `GET /api/v1/auth/api-keys` — list (metadata only, no key values)
  - `DELETE /api/v1/auth/api-keys/{id}` — revoke
- [ ] Keys scoped to a tenant and a set of permissions
- [ ] Key usage logged in audit trail
- [ ] Rate limiting per key
- [ ] Unit tests
