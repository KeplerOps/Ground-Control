---
title: "CI pipeline: Type checking (mypy + TypeScript tsc)"
labels: [foundation, ci-cd, quality]
phase: 0
priority: P0
---

## Description

Create a GitHub Actions workflow for static type checking on both the Python backend (mypy) and TypeScript frontend (tsc). Type checking catches a class of bugs that linters miss.

## References

- Issue #003 (Coding Standards — mypy strict, TypeScript strict)
- Architecture: Section 7 (Python 3.12+, TypeScript)

## Acceptance Criteria

- [ ] `.github/workflows/typecheck.yml` workflow:
  - Triggers on: push to `main`, pull requests
  - **Python job:**
    - `mypy backend/src/ --strict` with project-specific config from `pyproject.toml`
    - Reports type errors as GitHub annotations
  - **TypeScript job:**
    - `npx tsc --noEmit` — type check without emitting
    - Reports errors as GitHub annotations
- [ ] Both jobs must pass for PR merge
- [ ] Use incremental mypy caching (`--cache-dir`) via `actions/cache`
- [ ] Document how to run type checks locally: `make typecheck`

## Technical Notes

- mypy may need stubs for some dependencies: `types-redis`, `types-boto3`, etc.
- Add `mypy` plugin for `pydantic` and `sqlalchemy` in `pyproject.toml`:
  ```toml
  [tool.mypy]
  plugins = ["pydantic.mypy", "sqlalchemy.ext.mypy.plugin"]
  ```
- Type stubs should be dev dependencies, not runtime
