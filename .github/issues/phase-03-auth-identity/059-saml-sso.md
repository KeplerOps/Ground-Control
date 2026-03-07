---
title: "Implement SAML 2.0 SSO integration"
labels: [backend, auth, sso, enterprise]
phase: 3
priority: P1
---

## Description

Implement SAML 2.0 Service Provider (SP) for enterprise SSO with corporate IdPs (Okta, Azure AD, ADFS, Ping Identity).

## References

- Deployment: Section 5.1 (SAML 2.0 configuration)
- PRD: Section 8.1 (SAML 2.0 — SP/IdP initiated)
- User Stories: US-7.1 (Configure SSO)
- Use Cases: UC-07

## Acceptance Criteria

- [ ] SAML endpoints:
  - `GET /api/v1/auth/saml/metadata` — SP metadata XML
  - `POST /api/v1/auth/saml/acs` — Assertion Consumer Service
  - `GET /api/v1/auth/saml/slo` — Single Logout
- [ ] SP-initiated and IdP-initiated SSO flows
- [ ] Attribute mapping: email, display_name, groups (configurable)
- [ ] Signed requests (RSA-SHA256)
- [ ] JIT user provisioning on first SAML login
- [ ] Group-to-role mapping configurable per tenant
- [ ] SSO enforcement option (disable local login)
- [ ] Admin UI endpoint to configure SAML settings
- [ ] Integration tests with `python3-saml` or `pysaml2`
