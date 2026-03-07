---
title: "Implement OAuth2 client credentials flow for agents"
labels: [backend, auth, agents]
phase: 3
priority: P0
---

## Description

Implement OAuth2 client credentials grant for machine-to-machine authentication, primarily used by AI agents.

## References

- API Spec: Section 2.1 (Token Endpoint — client_credentials)
- Architecture: Section 4 (Agent authentication)
- User Stories: US-8.1 (Register an Agent)

## Acceptance Criteria

- [ ] Token endpoint: `POST /api/v1/auth/token` with `grant_type=client_credentials`
- [ ] Client authentication: client_id + client_secret (from agent registration)
- [ ] Scope validation: requested scopes must be subset of agent's allowed_scopes
- [ ] Returns JWT access token with agent identity claims
- [ ] Token includes `actor_type: "agent"` to distinguish from human tokens
- [ ] Client secret hashed with Argon2id in database
- [ ] Rate limiting per client_id
- [ ] Audit logging of token issuance
- [ ] Unit tests
