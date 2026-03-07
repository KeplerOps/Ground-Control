---
title: "Build end-to-end test suite (Playwright)"
labels: [testing, frontend, quality, production]
phase: 11
priority: P0
---

## Description

Build a comprehensive end-to-end test suite using Playwright that verifies critical user workflows through the UI.

## References

- Architecture: Section 7 (Testing — Playwright for E2E)

## Acceptance Criteria

- [ ] Playwright setup in `frontend/tests/e2e/`
- [ ] E2E test scenarios covering critical paths:
  - Login flow (local + SSO mock)
  - Create risk → view in register → update score
  - Create control → map to framework → view coverage
  - Create assessment campaign → execute test procedure → submit for review → approve
  - Upload evidence → link to control → view lineage
  - Create finding → add remediation plan → validate → close
  - Admin: create user → assign role → verify access
- [ ] CI integration: E2E tests run against Docker Compose environment
- [ ] Visual regression testing (optional: Playwright screenshots)
- [ ] Test data seeding for reproducible tests
- [ ] `make e2e` target
