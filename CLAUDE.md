# Ground Control — AI Development Conventions

## Project Overview

Ground Control is an open, self-hostable IT Risk Management (ITRM) platform. It replaces proprietary GRC tools (AuditBoard ITRM) with a modern, API-first, plugin-extensible system for managing IT risk assessments, control testing, evidence collection, and compliance reporting.

## Architecture

- **Clean Architecture**: `api/ -> domain/ <- infrastructure/`. Domain has zero framework imports.
- **Backend**: Python 3.12+, FastAPI, SQLAlchemy 2.0 async, Pydantic v2, structlog
- **Frontend**: React, TypeScript, Vite
- **Database**: PostgreSQL with Row-Level Security for tenant isolation
- **Package name**: `ground_control` (underscore, PEP 8)

## Repository Structure

```
backend/src/ground_control/   # Python backend (FastAPI)
frontend/src/                 # React + TypeScript frontend
sdks/                         # Python and TypeScript agent SDKs
plugins/                      # Built-in framework definitions and integrations
deploy/                       # Docker, Helm, Terraform
architecture/                 # ADRs, C4 models, policy-as-code
docs/                         # Design documentation
```

## Coding Standards

**Read `docs/CODING_STANDARDS.md` before writing any code.** Key rules:

### Python
- Formatter/Linter: `ruff` (line length 99)
- Type checker: `mypy --strict`
- All public functions require type annotations
- Google-style docstrings on public API boundaries only
- Imports: stdlib -> third-party -> local (enforced by ruff)

### TypeScript
- Formatter/Linter: `biome`
- Type checker: `tsc --strict`
- No `any` types — use `unknown` and narrow

### Cross-Cutting (enforced from day one)
- **Exceptions**: All inherit from `GroundControlError`. Domain never raises `HTTPException`.
- **Logging**: `structlog` with JSON output. Semantic event names. Never `print()`.
- **Audit**: Every state change is audit-logged in the same transaction.
- **Tenant isolation**: Every query filters by `tenant_id`. RLS is the safety net.
- **Schemas**: Separate `Create`, `Read`, `Update` variants. Never expose ORM models.

## Dependency Rule

This is the most important rule. Violations fail CI via `import-linter`.

- `domain/` imports NOTHING from `api/`, `infrastructure/`, FastAPI, or SQLAlchemy
- `api/` depends on `domain/` and `schemas/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`
- `schemas/`, `exceptions/`, `logging/`, `events/` are cross-cutting (importable by any layer)

## Testing

- `tests/unit/` — Domain logic only. No DB, no HTTP. Fakes for repos.
- `tests/integration/` — With real PostgreSQL (testcontainers).
- `tests/e2e/` — Playwright, full stack via Docker Compose.
- Test names describe behavior: `test_create_risk_fails_when_likelihood_out_of_range`
- Coverage: 80% domain, 70% api/infrastructure minimum.

## Git Conventions

- Branch from `dev`, PR into `dev`. `main` is production-ready.
- Commit messages: imperative mood (`Add risk scoring engine`)
- Every commit updates CHANGELOG.md
- PRs require passing CI: ruff + mypy + pytest + import-linter

## Common Commands

```bash
# Backend
cd backend && ruff check src/        # Lint
cd backend && ruff format src/       # Format
cd backend && mypy src/              # Type check
cd backend && pytest                 # Test

# Frontend
cd frontend && npm run lint          # Lint
cd frontend && npm run build         # Build
cd frontend && npm run test          # Test
```
