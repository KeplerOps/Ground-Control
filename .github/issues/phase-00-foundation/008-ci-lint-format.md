---
title: "CI pipeline: Linting and formatting checks"
labels: [foundation, ci-cd, quality]
phase: 0
priority: P0
---

## Description

Create a GitHub Actions workflow that runs linting and formatting checks on every push and pull request. This is the first line of defense for code quality.

## References

- Architecture: Section 7 (GitHub Actions for CI/CD)
- Issue #003 (Coding Standards)

## Acceptance Criteria

- [ ] `.github/workflows/lint.yml` workflow:
  - Triggers on: push to `main`, pull requests
  - Matrix strategy: Python linting, TypeScript linting
  - **Python job:**
    - Checkout, setup Python 3.12, install deps
    - `ruff check --output-format=github .` — linting with GitHub annotations
    - `ruff format --check .` — formatting verification
  - **TypeScript job:**
    - Checkout, setup Node 20, install deps
    - `npm run lint` — ESLint
    - `npx prettier --check .` — Prettier
  - **YAML/Markdown job:**
    - `yamllint` on `.yml` / `.yaml` files
    - `markdownlint-cli2` on `.md` files (with reasonable config)
- [ ] Workflow fails if any check fails
- [ ] Workflow runs in < 2 minutes for typical changes
- [ ] Status checks required for PR merge (document in branch protection rules)

## Technical Notes

- Use `ruff` native GitHub output format for inline PR annotations
- Cache pip/npm dependencies using `actions/cache` for speed
- Consider `concurrency` group to cancel stale runs on the same branch
