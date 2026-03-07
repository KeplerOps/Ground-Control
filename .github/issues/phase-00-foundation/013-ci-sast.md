---
title: "CI pipeline: SAST scanning (Semgrep + Bandit)"
labels: [foundation, ci-cd, security]
phase: 0
priority: P0
---

## Description

Implement Static Application Security Testing (SAST) in the CI pipeline using Semgrep (multi-language, rule-based) and Bandit (Python-specific). SAST analyzes source code for security vulnerabilities before deployment.

## References

- Architecture: Section 6 (Security Architecture — Supply chain)
- PRD: Section 7 (Non-Functional Requirements — security)

## Acceptance Criteria

- [ ] `.github/workflows/sast.yml` workflow:
  - Triggers on: push to `main`, pull requests, weekly schedule
  - **Semgrep job:**
    - Uses `semgrep/semgrep-action`
    - Rulesets: `p/security-audit`, `p/python`, `p/typescript`, `p/owasp-top-ten`, `p/secrets`
    - Findings reported as GitHub annotations
    - Upload SARIF results to GitHub Security tab
  - **Bandit job:**
    - Runs `bandit -r backend/src/ -f sarif -o bandit.sarif`
    - Excludes test files
    - Upload SARIF to GitHub Security tab
  - **CodeQL job (optional, GitHub-native):**
    - `github/codeql-action/init` + `analyze` for Python and JavaScript
- [ ] SAST findings block PR merge if severity ≥ High
- [ ] Baseline file for known/accepted findings (`.semgrepignore`, `bandit.yaml`)
- [ ] `make security-scan` target for local SAST runs

## Technical Notes

- Semgrep is fast and supports custom rules — can add project-specific rules later (e.g., "never use `eval()`", "always validate tenant_id")
- Bandit covers Python-specific issues (SQL injection, hardcoded passwords, insecure crypto)
- SARIF format enables unified view in GitHub Security tab
- Consider adding `eslint-plugin-security` for TypeScript-specific findings
