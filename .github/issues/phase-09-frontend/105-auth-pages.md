---
title: "Build authentication pages (login, SSO, password management)"
labels: [frontend, ui, auth]
phase: 9
priority: P0
---

## Description

Build the frontend authentication pages: login form, SSO redirect, password change, and MFA verification.

## References

- User Stories: US-7.1 (Configure SSO)
- Deployment: Section 5 (SSO Configuration)

## Acceptance Criteria

- [ ] Login page: email + password form, "Sign in with SSO" button
- [ ] SSO redirect flow (OIDC/SAML)
- [ ] Password change page
- [ ] MFA verification page (TOTP input)
- [ ] First-time setup page (initial admin creation)
- [ ] Error handling: invalid credentials, account locked, SSO errors
- [ ] Responsive design (mobile-friendly)
- [ ] Accessibility: form labels, keyboard navigation, screen reader support
