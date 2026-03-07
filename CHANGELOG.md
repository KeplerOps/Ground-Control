# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
