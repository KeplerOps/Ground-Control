---
title: "CI pipeline: DAST scanning (OWASP ZAP)"
labels: [foundation, ci-cd, security]
phase: 0
priority: P1
---

## Description

Implement Dynamic Application Security Testing (DAST) using OWASP ZAP to test the running application for vulnerabilities. DAST complements SAST by testing the deployed application as a black box, finding runtime vulnerabilities like XSS, CSRF, injection, and misconfigurations.

## References

- Architecture: Section 6 (Security Architecture — defense in depth)
- PRD: Section 7 (Non-Functional Requirements — security)

## Acceptance Criteria

- [ ] `.github/workflows/dast.yml` workflow:
  - Triggers on: push to `main` (not PRs — too slow), weekly schedule
  - Spins up application + services via Docker Compose
  - Waits for health check to pass
  - Runs OWASP ZAP baseline scan against `http://localhost:8000`
  - Runs ZAP API scan using OpenAPI spec (`/api/v1/openapi.json`)
  - Results uploaded as SARIF to GitHub Security tab
  - Report artifact saved for download
- [ ] ZAP configuration file (`.zap/rules.tsv`) for:
  - Tuning false positives
  - Setting alert thresholds (FAIL on High, WARN on Medium)
- [ ] DAST scan completes in < 15 minutes
- [ ] Known false positives documented and suppressed

## Technical Notes

- Use `zaproxy/action-baseline` for baseline scan and `zaproxy/action-api-scan` for API scan
- The API scan uses the OpenAPI spec to discover and test all endpoints
- DAST requires a running application — use `docker compose up` in CI
- Consider running DAST only on `main` to avoid slowing PR feedback loops
- ZAP can authenticate — configure it with a test user for authenticated scanning
