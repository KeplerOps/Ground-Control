# ADR-009: Coding Standards and Tooling

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control is a multi-language monorepo (Python backend, TypeScript frontend). Without enforced coding standards, style drift and inconsistent patterns will slow development and increase bugs. We need automated tooling that enforces standards in CI and at commit time, not just documentation.

## Decision

### Python Backend

- **Linter and formatter**: `ruff` (replaces flake8, isort, black in a single binary)
  - Line length: 100
  - Target: Python 3.12
  - Rule sets: pycodestyle (E), pyflakes (F), isort (I), pep8-naming (N), bugbear (B), annotations (ANN), security (S), pydocstyle (D), pyupgrade (UP)
- **Type checker**: `mypy` with `strict = true`
  - Disallow untyped defs, no implicit optional
- **Docstrings**: Google style convention, required on public API boundaries only
- **Import ordering**: stdlib, third-party, local (enforced by ruff isort rules)

### TypeScript Frontend

- **Linter and formatter**: `biome` (replaces ESLint + Prettier in a single binary)
- **Type checker**: `tsc --strict`
- **No `any` types** — use `unknown` and narrow

### Shared

- `.editorconfig` for consistent whitespace (4 spaces Python, 2 spaces TS/YAML)
- Pre-commit hooks run ruff, mypy, and gitleaks before every commit
- CI pipeline enforces all checks — failures block merge

### Naming Conventions (Python)

| Element | Convention |
|---------|-----------|
| Modules | `snake_case` |
| Classes | `PascalCase` |
| Functions/methods | `snake_case` |
| Constants | `UPPER_SNAKE_CASE` |
| Private | `_leading_underscore` |

## Consequences

### Positive

- Single tool per language (ruff, biome) reduces config complexity and speeds up checks
- Strict type checking catches bugs before runtime
- Automated enforcement means standards are not optional
- Consistent style reduces cognitive load during code review

### Negative

- Strict mypy can require verbose type annotations in some cases
- Developers must learn ruff/biome if unfamiliar (mitigated: both are fast and well-documented)
- Some valid code patterns are rejected by strict rules (use targeted ignores with justification)

### Risks

- Tool upgrades may introduce new lint rules that break CI (mitigated: pin versions in pre-commit and pyproject.toml)
