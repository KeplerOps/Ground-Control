# Contributing to Ground Control

## Getting Started

1. Fork and clone the repository
2. Create a feature branch from `dev`: `git checkout -b feature/your-feature`
3. Make your changes
4. Push and open a PR against `dev`

## Branch Strategy

- `main` — production-ready, protected
- `dev` — integration branch, all PRs target this
- `feature/*` — feature branches, branched from `dev`

## Coding Standards

Read [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) for the complete reference. Key points below.

### Python Backend

| Tool | Purpose | Command |
|------|---------|---------|
| `ruff check` | Linting | `cd backend && ruff check src/` |
| `ruff format` | Formatting | `cd backend && ruff format src/` |
| `mypy` | Type checking | `cd backend && mypy src/` |
| `pytest` | Testing | `cd backend && pytest` |

- **Line length**: 100
- **Type hints**: Required on all public functions and methods
- **Docstrings**: Google style, on public API boundaries only
- **Imports**: stdlib, third-party, local (enforced by ruff)
- **No `Any`** unless unavoidable (comment why)
- **No `print()`** — use `structlog`

### TypeScript Frontend

| Tool | Purpose | Command |
|------|---------|---------|
| `biome check` | Lint + format | `cd frontend && npx biome check src/` |
| `tsc` | Type checking | `cd frontend && npx tsc --noEmit` |

- **Strict mode**: `strict: true` in `tsconfig.json`
- **No `any`** — use `unknown` and narrow

### Naming Conventions (Python)

| Element | Convention | Example |
|---------|-----------|---------|
| Modules | `snake_case` | `risk_service.py` |
| Classes | `PascalCase` | `RiskService` |
| Functions/methods | `snake_case` | `create_risk()` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Private | `_leading_underscore` | `_validate_score()` |

## Architecture Rules

The dependency rule is enforced by `import-linter` in CI:

```
api/ -> domain/ <- infrastructure/
```

- `domain/` has **zero** framework imports beyond Django models (no views, no URL routing)
- `api/` depends on `domain/` and `schemas/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`

See [ADR-008](architecture/adrs/008-clean-architecture.md) for rationale.

## Commit Messages

- Imperative mood: `Add risk scoring engine` not `Added risk scoring engine`
- Every commit updates `CHANGELOG.md`

## Pull Requests

- Target `dev`, not `main`
- PRs require passing CI (lint + typecheck + tests + import-linter)
- No coverage regression
- Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md)

## Testing

```
tests/
├── unit/          # Domain logic only. No DB, no HTTP.
├── integration/   # With real PostgreSQL (testcontainers).
└── e2e/           # Playwright, full stack.
```

- Test names describe behavior: `test_create_risk_fails_when_likelihood_out_of_range`
- Coverage minimums: 80% domain, 70% api/infrastructure
- Tests are independent — no shared mutable state
