---
title: "Implement MFA support (TOTP + WebAuthn)"
labels: [backend, auth, security]
phase: 3
priority: P2
---

## Description

Add multi-factor authentication support for local accounts using TOTP (authenticator apps) and WebAuthn (hardware security keys, biometrics).

## References

- PRD: Section 8.1 (Authentication — MFA: TOTP and WebAuthn)
- Deployment: Section 9 (Security Hardening — Enable MFA)

## Acceptance Criteria

- [ ] TOTP enrollment: generate secret, display QR code, verify first code
- [ ] TOTP verification on login (after password)
- [ ] Recovery codes (one-time use, generated at enrollment)
- [ ] WebAuthn registration and authentication (passkeys)
- [ ] MFA can be required per-tenant (admin setting)
- [ ] MFA bypass for SSO users (IdP handles MFA)
- [ ] User model: `mfa_enabled`, `mfa_method`, `mfa_secret` (encrypted)
- [ ] Unit tests
