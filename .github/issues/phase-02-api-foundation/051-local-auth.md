---
title: "Implement local authentication (Argon2id + login flow)"
labels: [api, backend, auth, security]
phase: 2
priority: P0
---

## Description

Implement local (username/password) authentication with Argon2id password hashing, login/logout endpoints, and password management.

## References

- Architecture: Section 4 (Authentication — local path)
- Architecture: Section 6.2 (Data Protection — Argon2id hashing)
- API Spec: Section 2 (Authentication — token endpoint)
- User Stories: US-7.1 (Configure SSO — local as fallback)

## Acceptance Criteria

- [ ] Password hashing using Argon2id (via `passlib`)
- [ ] Login endpoint: `POST /api/v1/auth/login` — email + password → JWT tokens
- [ ] Registration endpoint: `POST /api/v1/auth/register` (admin-only or first-user bootstrap)
- [ ] Password change: `POST /api/v1/auth/change-password`
- [ ] Password reset flow: request → email token → reset
- [ ] Password validation: minimum 12 chars, complexity configurable
- [ ] Account lockout after N failed attempts (configurable, default 5)
- [ ] Login events audit-logged (success and failure)
- [ ] Rate limiting on auth endpoints (stricter than general API)
- [ ] Unit tests for hashing, login flow, lockout
- [ ] Contracts: password never stored in plaintext, never returned in API responses
