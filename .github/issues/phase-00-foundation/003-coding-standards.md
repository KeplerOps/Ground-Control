---
title: "Define and enforce coding standards and style guide"
labels: [foundation, devex, quality]
phase: 0
priority: P0
---

## Description

Establish coding standards for both the Python backend and TypeScript frontend. Document conventions in a `CONTRIBUTING.md` and enforce via tooling (linters, formatters, pre-commit hooks). Standards cover naming, typing, imports, error handling, logging patterns, and testing conventions.

## References

- Architecture: Section 7 (Technology Stack — Python 3.12+, TypeScript, FastAPI)
- PRD: Section 7 (Non-Functional Requirements — maintainability)

## Acceptance Criteria

### Python Backend

- [ ] `ruff` configured for linting + formatting (replaces flake8, isort, black)
  - Line length: 100
  - Target: Python 3.12
  - Rules: pycodestyle (E), pyflakes (F), isort (I), pep8-naming (N), bugbear (B), type annotations (ANN), security (S)
- [ ] `mypy` configured for strict type checking
  - `strict = true` in `pyproject.toml`
  - Disallow untyped defs, no implicit optional
- [ ] Naming conventions documented:
  - Modules: `snake_case`
  - Classes: `PascalCase`
  - Functions/methods: `snake_case`
  - Constants: `UPPER_SNAKE_CASE`
  - Private: `_leading_underscore`
- [ ] Import ordering: stdlib → third-party → local (enforced by ruff)
- [ ] Docstring format: Google style (enforced by ruff D rules)
- [ ] Type hints required on all public functions and methods

### TypeScript Frontend

- [ ] ESLint configured with `@typescript-eslint` strict rules
- [ ] Prettier configured (single quotes, trailing commas, 100 line length)
- [ ] TypeScript `strict: true` in `tsconfig.json`
- [ ] React-specific: `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`

### Shared

- [ ] `.editorconfig` for consistent whitespace (2 spaces for TS/YAML, 4 spaces for Python)
- [ ] `CONTRIBUTING.md` documenting all standards
- [ ] Standards are an ADR: `architecture/adrs/009-coding-standards.md`

## Technical Notes

- Prefer `ruff` over individual tools — single fast binary for Python linting and formatting
- `mypy` strict mode catches many bugs at type-check time; use `# type: ignore[code]` sparingly with justification
- All new code must pass type checking before merge
