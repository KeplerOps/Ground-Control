---
title: "Implement SCIM 2.0 provisioning endpoints"
labels: [backend, auth, sso, enterprise]
phase: 3
priority: P1
---

## Description

Implement SCIM 2.0 endpoints for automated user and group provisioning/deprovisioning from identity providers.

## References

- Deployment: Section 5.3 (SCIM 2.0 Provisioning)
- PRD: Section 8.1 (SCIM 2.0)
- User Stories: US-7.2 (SCIM provisioning syncs users)

## Acceptance Criteria

- [ ] SCIM 2.0 endpoints at `/api/v1/scim/v2/`:
  - `/Users` — CRUD + List + Filter
  - `/Groups` — CRUD + List
  - `/Schemas` — schema discovery
  - `/ServiceProviderConfig` — capabilities
- [ ] Bearer token authentication for SCIM endpoint
- [ ] User create → creates Ground Control user with `auth_provider: "scim"`
- [ ] User deactivate → deactivates Ground Control user (preserves data)
- [ ] Group sync → maps to Ground Control roles
- [ ] Attribute mapping: userName=email, displayName, active
- [ ] Patch support (SCIM PATCH operations)
- [ ] Integration tests with sample SCIM payloads
