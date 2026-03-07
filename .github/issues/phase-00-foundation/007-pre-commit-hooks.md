---
title: "Configure pre-commit hooks and editor settings"
labels: [foundation, devex, quality]
phase: 0
priority: P1
---

## Description

Set up `pre-commit` framework to enforce code quality gates before commits reach CI. Configure hooks for both Python and TypeScript code.

## References

- Issue #003 (Coding Standards)
- Issue #004 (Python Backend Scaffold)

## Acceptance Criteria

- [ ] `.pre-commit-config.yaml` at repo root with hooks:
  - `ruff` — lint and format Python
  - `mypy` — type check (optional, can be slow; may run on CI only)
  - `eslint` — lint TypeScript/React
  - `prettier` — format TypeScript/YAML/JSON/Markdown
  - `check-yaml` — validate YAML files
  - `check-json` — validate JSON files
  - `check-toml` — validate TOML files
  - `detect-secrets` — prevent accidental secret commits
  - `trailing-whitespace` — strip trailing whitespace
  - `end-of-file-fixer` — ensure files end with newline
  - `check-merge-conflict` — detect unresolved merge markers
  - `check-added-large-files` — block files > 1MB (configurable)
- [ ] `.editorconfig` at repo root:
  - `root = true`
  - Python: 4 spaces, 100 char line length
  - TypeScript/JavaScript: 2 spaces
  - YAML: 2 spaces
  - Markdown: trailing whitespace allowed (for line breaks)
- [ ] Documentation in `CONTRIBUTING.md` on installing pre-commit hooks
- [ ] `make hooks` target to install pre-commit hooks

## Technical Notes

- `detect-secrets` uses the `yelp/detect-secrets` tool — initialize baseline with `detect-secrets scan > .secrets.baseline`
- Consider making `mypy` a CI-only check if it's too slow for pre-commit
