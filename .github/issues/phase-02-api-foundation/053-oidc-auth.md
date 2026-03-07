---
title: "Implement OIDC authentication integration"
labels: [api, backend, auth, sso]
phase: 2
priority: P0
---

## Description

Implement OpenID Connect (OIDC) authentication using Authorization Code flow with PKCE. Supports corporate IdPs (Okta, Azure AD/Entra ID, Google).

## References

- Architecture: Section 4 (Authentication — OIDC)
- Deployment: Section 5.2 (OIDC Configuration)
- PRD: Section 8.1 (Authentication — OIDC)
- User Stories: US-7.1 (Configure SSO)
- Use Cases: UC-07 (Configure SSO)

## Acceptance Criteria

- [ ] OIDC endpoints:
  - `GET /api/v1/auth/oidc/authorize` — initiates OIDC flow (redirect to IdP)
  - `GET /api/v1/auth/oidc/callback` — handles IdP callback, exchanges code for tokens
- [ ] OIDC configuration stored per tenant (issuer, client_id, client_secret, scopes)
- [ ] ID token validation: issuer, audience, expiry, nonce, signature
- [ ] User provisioning: JIT (Just-In-Time) — create user on first login if not exists
- [ ] Claim mapping: email, display_name, groups → roles
- [ ] PKCE support for public clients (SPA)
- [ ] OIDC discovery: auto-fetch `.well-known/openid-configuration`
- [ ] Admin endpoint to configure OIDC settings
- [ ] Integration tests with mocked OIDC provider
