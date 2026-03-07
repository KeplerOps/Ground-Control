---
title: "Implement agent registration and lifecycle management"
labels: [backend, agents, api]
phase: 7
priority: P0
---

## Description

Build the agent registration system — registering AI agents with credentials, permissions, and lifecycle management.

## References

- Data Model: Section 2.14 (Agent Registration)
- PRD: Section 5 (Agent-First Design)
- User Stories: US-8.1 (Register an Agent)
- API Spec: Section 4.9 (Agents)

## Acceptance Criteria

- [ ] Agent registration: `POST /api/v1/agents` creates agent with name, description, owner, allowed_scopes
- [ ] OAuth2 client credentials generated (client_id + client_secret)
- [ ] Client secret returned once (on creation), stored as Argon2id hash
- [ ] Agent assigned a role that limits permissions
- [ ] Agent lifecycle: active, suspended, revoked
- [ ] Agent CRUD endpoints per API spec
- [ ] Agent credential rotation: `POST /api/v1/agents/{id}/rotate-secret`
- [ ] All agent registration/modification audit-logged
- [ ] Unit tests
