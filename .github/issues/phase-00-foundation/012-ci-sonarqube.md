---
title: "CI pipeline: SonarQube integration for code quality"
labels: [foundation, ci-cd, quality, security]
phase: 0
priority: P1
---

## Description

Integrate SonarQube (or SonarCloud for open source) into the CI pipeline for continuous code quality and security analysis. SonarQube provides code smell detection, bug detection, security vulnerability scanning, coverage tracking, and technical debt measurement.

## References

- PRD: Section 7 (Non-Functional Requirements — quality, security)
- Issue #010 (Unit Tests — coverage reports feed into Sonar)

## Acceptance Criteria

- [ ] `.github/workflows/sonar.yml` workflow:
  - Triggers on: push to `main`, pull requests
  - Runs SonarCloud analysis (free for open source)
  - Uploads Python coverage report (`coverage.xml` from pytest)
  - Uploads TypeScript coverage report
- [ ] `sonar-project.properties` at repo root:
  - Project key, organization
  - Source directories: `backend/src`, `frontend/src`
  - Test directories: `backend/tests`, `frontend/src/**/*.test.*`
  - Exclusions: migrations, generated files, vendor
  - Coverage report paths
  - Python-specific: `sonar.python.version=3.12`
- [ ] Quality gate configured:
  - New code coverage > 80%
  - No new bugs (A rating)
  - No new vulnerabilities (A rating)
  - No new security hotspots (A rating)
  - Technical debt ratio < 5%
  - Duplication < 3%
- [ ] Quality gate status reported on PRs
- [ ] SonarCloud badge in README

## Technical Notes

- Use `SonarSource/sonarcloud-github-action` for analysis
- SonarCloud is free for public repos; for private, use self-hosted SonarQube
- The `SONAR_TOKEN` secret must be configured in GitHub repo settings
- Coverage reports must be generated before Sonar analysis runs
