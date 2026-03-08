# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.11.0] - 2026-03-08

### Added

- CI pipeline (`.github/workflows/ci.yml`): lint, typecheck, and test jobs run in parallel on push/PR to `main`/`dev`

### Fixed

- Mypy override for `settings.base` — `# type: ignore[misc]` needed for pre-commit per-file check but flagged as unused in full-project check

## [0.10.0] - 2026-03-08

### Added

- Docker Compose dev environment with PostgreSQL 16 (Apache AGE 1.6.0) and Redis 7
- `.env.example` documenting all `GC_` environment variables
- Makefile `up` and `down` targets for managing Docker Compose services
- ADR-005: Apache AGE for graph database capabilities (chose over Neo4j for operational simplicity)

### Changed

- Parse `GC_DATABASE_URL` dynamically into Django `DATABASES` setting (was hardcoded)
- Rewrite all operational docs to reflect actual codebase state (remove aspirational content)
- Rewrite DEPLOYMENT.md as dev environment setup guide
- Rewrite ARCHITECTURE.md to document current stack and project structure
- Trim CODING_STANDARDS.md to enforceable rules only
- Rewrite README.md: accurate structure, status section, links to correct paths
- Update CONTRIBUTING.md with local dev setup instructions

## [0.9.0] - 2026-03-08

### Added

- Fresh ADR framework with template (`architecture/adrs/000-template.md`) and clean index
- ADR-001: Python 3.12+ with Django and django-ninja for Backend
- ADR-002: PostgreSQL as Primary Database
- ADR-003: Design by Contract with icontract
- ADR-004: Code Quality Toolchain
- Restored `docs/CODING_STANDARDS.md` from archive
- 7 new phase-0 bootstrap issues (#158–#164) for getting Django deployment-ready

### Changed

- Project pivot: Ground Control reframed from ITRM platform to neurosymbolic constraint infrastructure, dogfooded on itself
- Archived pre-pivot work into `archive/` (docs, tools, architecture ADRs)
- ADR numbering reset — old ADRs (001–010) archived, new series starts at 001

### Fixed

- Django settings: removed references to `django_tenants` and `oauth2_provider` (not in dependencies, caused `ModuleNotFoundError` on startup)
- Django settings: switched database engine from `django_tenants.postgresql_backend` to `django.db.backends.postgresql`
- `manage.py check` now passes

### Removed

- All 131 GitHub issues from old roadmap (historical record preserved in `archive/tools/issue-graph/.issue_cache.json`)
- `docs/` moved to `archive/docs/` (personas, glossary, requirements, roadmap, coding standards, user stories, API/deployment docs)
- `tools/` moved to `archive/tools/` (issue-graph, strictdoc)
- `architecture/` moved to `archive/architecture/` (ADRs, C4 diagrams, policies)
- `django_tenants` config from settings (SHARED_APPS/TENANT_APPS pattern, TenantMainMiddleware, TenantSyncRouter, TENANT_MODEL/TENANT_DOMAIN_MODEL)
- `oauth2_provider` from INSTALLED_APPS

## [0.8.0] - 2026-03-08

### Added

- `tools/issue-graph/` — standalone NetworkX-based GitHub issue dependency graph analyzer
  - Own pyproject.toml, venv, and Makefile (`make setup && make run`)
  - Fetches issues via `gh` CLI, builds directed dependency graph
  - Validates for cycles, cross-phase backward deps, orphans, stale tech references
  - Computes critical path and top blocking issues
  - `--sdoc-gaps`: checks sdoc ↔ GitHub issue traceability (both directions)
  - `--cross-check`: validates sdoc Parent relations against issue dependencies, detects self-referencing parents, backward wave deps
  - Exports graph as JSON for further analysis
- `docs/roadmap/RATIONALIZATION.md` — issue rationalization plan
  - Reorganizes 124 open issues from 12 phases into 10 waves with validated dependency ordering
  - Identifies 8 issues to close, 26 to defer, 36 to rewrite for Django
  - Wave ordering validated against dependency graph (no backward deps)
- `tools/strictdoc/` — StrictDoc requirements management setup
  - Own venv and Makefile (`make setup && make server`)
  - Web UI for browsing and editing requirements
- `docs/requirements/project.sdoc` — product requirements (replaces PRD.md)
  - 80 requirements organized into 10 waves with parent-child traceability
  - All 131 open GitHub issues mapped to requirements via COMMENT field
  - Validated by StrictDoc (no broken links, no cycles)
  - sdoc ↔ issue dependency graph fully synced (125 edges)
- `docs/personas/` — one file per persona (7 personas extracted from PRD)
- `docs/glossary.md` — terminology reference
- 7 new GitHub issues created for PRD requirements that had no issue (#151-#157)

### Changed

- Makefile: Replace uvicorn command with `manage.py runserver` (last FastAPI remnant)
- Rewrite issue #33 for django-ninja context (was FastAPI Pydantic/DI)
- Rewrite issue #39 to use Django permissions/groups (was premature ABAC/OPA)
- Issue #44: rewritten for Django ORM, added control effectiveness acceptance criteria
- Issue #49: rewritten for Django ORM + django-storages, added 500MB artifact size limit
- Issue #133: added encryption-at-rest (AES-256), TLS 1.3, and HA acceptance criteria
- 81 issues updated with `## Dependencies` section synced from sdoc Parent relations

### Removed

- `docs/PRD.md` — superseded by `docs/requirements/project.sdoc`
- `django-tenants` from production deps — premature for on-prem single-tenant v0.1
- `django-oauth-toolkit` from production deps — OAuth2 is v0.4 scope, Django auth sufficient for v0.1
- `deal` from dev deps — redundant with icontract
- `respx` from dev deps — HTTPX mock library not needed with Django test client
- `pytest-asyncio` from dev deps — Django tests are sync-first
- `asyncio_mode = "auto"` from pytest config
- Closed issues #55 (FastAPI scaffold), #34 (SQLAlchemy engine), #35 (Alembic migrations) as not_planned

## [0.7.0] - 2026-03-08

### Changed

- Switch backend framework from FastAPI to Django + django-ninja (ADR-010 supersedes ADR-001)
- Replace SQLAlchemy + Alembic with Django ORM and built-in migrations
- Replace manual auth stack (python-jose, passlib) with Django auth + django-oauth-toolkit
- Update `backend/pyproject.toml` dependencies for Django ecosystem
- Update CODING_STANDARDS.md, ARCHITECTURE.md, CONTRIBUTING.md for Django references

### Added

- ADR-010: Evaluate Django framework — documents rationale for switching
- Django project structure: settings (base, test), urls.py, asgi.py, wsgi.py, manage.py
- django-tenants for multi-tenancy, django-auditlog for audit trail, django-storages for S3
- django-q2 for background task processing
- pytest-django and django-stubs in dev dependencies

## [0.6.1] - 2026-03-07

### Added

- `backend/tests/unit/test_package.py` package importability and version test
- CI: Python 3.12 setup, uv install, and pytest coverage in SonarCloud workflow

### Fixed

- SonarCloud quality gate failure: configured `sonar.sources`, `sonar.tests`, and coverage report path
- SonarCloud now receives coverage XML from pytest-cov

## [0.6.0] - 2026-03-07

### Added

- `backend/pyproject.toml` with full dependency declarations (FastAPI, SQLAlchemy, Pydantic, structlog, etc.) and optional dependency groups (dev, test, docs)
- `backend/src/ground_control/__init__.py` with `__version__`
- `backend/src/ground_control/py.typed` PEP 561 marker for typed package
- `backend/tests/conftest.py` shared test fixtures stub
- Root `Makefile` with common commands (install, lint, format, test, dev, clean)
- `uv` support with `pip` fallback in Makefile

## [0.5.0] - 2026-03-07

### Added

- `backend/pyproject.toml` with ruff (line length 100, Python 3.12, security/typing/style rules) and mypy strict config
- `CONTRIBUTING.md` documenting coding standards, architecture rules, branch strategy, and testing conventions
- ADR-009: Coding Standards and Tooling

### Changed

- Line length updated from 99 to 100 in CODING_STANDARDS.md, .editorconfig, and CLAUDE.md

## [0.4.0] - 2026-03-07

### Added

- ADR framework with MADR template (`architecture/adrs/000-template.md`)
- ADR index (`architecture/adrs/README.md`)
- Initial ADRs for foundational decisions:
  - ADR-001: Python 3.12+ with FastAPI for backend
  - ADR-002: PostgreSQL 16+ as primary database
  - ADR-003: API-first design (REST)
  - ADR-004: Plugin architecture for extensibility
  - ADR-005: Event-driven architecture with domain events
  - ADR-006: Multi-tenancy strategy (shared schema default)
  - ADR-007: Agent-first design (AI agents as first-class actors)
  - ADR-008: Clean architecture (API / Domain / Infrastructure layers)

## [0.3.0] - 2026-03-07

### Added

- Monorepo directory structure: backend, frontend, sdks, plugins, deploy, architecture
- `CLAUDE.md` with AI-assisted development conventions
- `.editorconfig` for consistent whitespace across Python, TypeScript, YAML, Markdown
- GitHub issue templates (bug report, feature request)
- GitHub pull request template with coding standards checklist
- Placeholder `__init__.py` and `.gitkeep` files for all directories
- Repository structure overview in README.md
- Node.js / frontend entries in `.gitignore`

## [0.2.0] - 2026-03-07

### Added

- Complete ITRM platform design documentation:
  - Product Requirements Document (PRD)
  - System Architecture (Clean Architecture, shared-schema multi-tenancy)
  - Data Model (entity-relationship model, typed foreign keys, audit log)
  - API Specification (REST, flat JSON responses, PATCH via RFC 7396)
  - Deployment Guide (Docker Compose, Kubernetes Helm, SSO)
  - User Stories with MVP markers and Use Cases (UML)
- Coding Standards document with cross-cutting concerns (exceptions, logging, audit, schemas, tenant context)
- Formal methods infrastructure (Coq/Rocq proof targets for audit log, RBAC, state machines, tenant isolation)
- 129 implementation issues across 12 phases (phase-0 through phase-11)
- Issue creation script (`scripts/create-github-issues.sh`) with label management and rate limiting
- Pre-commit hooks (ruff, mypy, gitleaks, pytest)
- SonarCloud integration (GitHub Actions workflow, sonar-project.properties)
- MCP development tooling issue (rocq-mcp, AWS MCP)

### Changed

- License changed from Apache-2.0 to MIT

## [0.1.0] - 2025-01-15

### Added

- Initial repository structure
- GitHub Actions workflows for quality and security checks
- Pre-commit configuration
- Project documentation (README, LICENSE)
