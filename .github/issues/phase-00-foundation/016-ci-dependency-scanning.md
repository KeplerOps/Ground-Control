---
title: "CI pipeline: Dependency and license scanning"
labels: [foundation, ci-cd, security]
phase: 0
priority: P1
---

## Description

Implement dependency vulnerability scanning and license compliance checking in CI. This ensures known vulnerable dependencies are caught before deployment and all dependencies comply with the project's Apache-2.0 license.

## References

- Architecture: Section 6 (Security — supply chain, SBOM generation)

## Acceptance Criteria

- [ ] `.github/workflows/deps.yml` workflow:
  - Triggers on: push to `main`, pull requests, weekly schedule
  - **Python dependency scanning:**
    - `pip-audit` — checks against OSV/PyPI advisories
    - `safety` (optional) — checks against Safety DB
  - **Node dependency scanning:**
    - `npm audit --audit-level=high`
  - **License scanning:**
    - `pip-licenses --format=json --with-urls` — export Python license info
    - `license-checker` (npm) — export Node license info
    - Fail if any dependency uses GPL, AGPL, or other copyleft licenses (incompatible with Apache-2.0)
  - **GitHub Dependabot:**
    - `.github/dependabot.yml` configured for Python (pip), npm, GitHub Actions, Docker
    - Weekly update schedule
    - Grouped updates for minor/patch versions
- [ ] SBOM generation (CycloneDX format) on each release
- [ ] `make audit` target for local dependency scanning
- [ ] Renovate or Dependabot auto-creates PRs for dependency updates

## Technical Notes

- `pip-audit` uses the OSV database — more comprehensive than `safety` for open-source
- License allowlist: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, PSF, MPL-2.0
- License denylist: GPL-2.0, GPL-3.0, AGPL-3.0, SSPL, EUPL
- SBOM (Software Bill of Materials) is increasingly required for enterprise adoption
