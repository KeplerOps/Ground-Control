---
title: "Implement JWT token management (access + refresh)"
labels: [api, backend, auth, security]
phase: 2
priority: P0
---

## Description

Implement JWT access and refresh token issuance, validation, and rotation. All authenticated API access requires a valid JWT.

## References

- Architecture: Section 4 (Authentication — JWT Access Token)
- API Spec: Section 2 (Authentication — token usage, bearer header)

## Acceptance Criteria

- [ ] Token service: `TokenService`:
  - `create_access_token(user_id, tenant_id, roles, scopes) → str`
  - `create_refresh_token(user_id) → str`
  - `validate_token(token) → TokenPayload`
  - `refresh(refresh_token) → (access_token, refresh_token)` — rotation
  - `revoke(token)` — adds to denylist
- [ ] Access token payload: `sub` (user_id), `tenant_id`, `roles`, `scopes`, `exp`, `iat`, `jti`
- [ ] Access token lifetime: configurable (default 60 min)
- [ ] Refresh token lifetime: configurable (default 30 days)
- [ ] Refresh token rotation: old refresh token invalidated on use
- [ ] Token denylist in Redis (for revocation, with TTL = token max lifetime)
- [ ] `get_current_user` dependency extracts and validates JWT from `Authorization: Bearer` header
- [ ] Unit tests for token creation, validation, expiry, revocation, rotation

## Technical Notes

- Use `python-jose[cryptography]` for JWT encoding/decoding
- Support both HS256 (shared secret) and RS256 (asymmetric) — configurable
- Token denylist is checked on every request — must be fast (Redis GET)
