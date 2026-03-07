---
title: "CI pipeline: Unit and integration test runner"
labels: [foundation, ci-cd, testing]
phase: 0
priority: P0
---

## Description

Create a GitHub Actions workflow that runs the test suites for both backend (pytest) and frontend (vitest). Include coverage reporting and thresholds.

## References

- Architecture: Section 7 (pytest + Playwright for testing)
- PRD: Section 7 (Non-Functional Requirements)

## Acceptance Criteria

- [ ] `.github/workflows/test.yml` workflow:
  - Triggers on: push to `main`, pull requests
  - **Python job:**
    - Services: PostgreSQL 16, Redis 7 (GitHub Actions service containers)
    - Setup Python 3.12, install deps
    - `pytest --cov=ground_control --cov-report=xml --cov-report=term -v`
    - Upload coverage report as artifact
    - Coverage threshold: 80% minimum (fail if below)
  - **TypeScript job:**
    - `npm run test -- --coverage`
    - Coverage threshold: 80% minimum
- [ ] Test results displayed in PR checks (use pytest-github-actions-annotate-failures or similar)
- [ ] Coverage reports uploadable to SonarQube (see #012)
- [ ] Workflow uses concurrency groups to cancel stale runs
- [ ] `make test` and `make test-cov` targets in Makefile

## Technical Notes

- Use `pytest-asyncio` with `asyncio_mode = "auto"` for async test support
- Use `factory-boy` for test data factories
- Use `respx` for mocking httpx calls
- PostgreSQL service should use same version as production
- Tests should use a test database that is created/destroyed per session
