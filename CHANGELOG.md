# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.22.0] - 2026-03-09

### Changed

- Removed JML contracts from 6 L0 CRUD methods in RequirementService per ADR-012 pre-alpha policy (L0 = working code + tests, no contracts). L1 contracts retained on transitionStatus, archive, createRelation
- Removed `VERIFIES` from `RelationType` enum — "verifies" is an artifact-to-requirement relationship belonging on `TraceabilityLink.LinkType` (Phase 1C, ADR-014), not a requirement-to-requirement edge
- Rewrote `CONTRIBUTING.md` for Java 21 / Spring Boot 3.4 / Gradle (was Python/Django)
- Rewrote `docs/architecture/ARCHITECTURE.md` for current stack and mission (was Python/Django)
- Updated `README.md` mission statement to reflect verification orchestration + graph traceability

### Added

- `RequirementControllerTest`: `@WebMvcTest` unit tests covering all 9 controller endpoints, exception handler (404/409/422/401/403/500), and DTO mapping
- `RequestLoggingFilterTest`: unit tests for MDC request_id binding
- `ExceptionHierarchyTest`: unit tests for AuthenticationException, AuthorizationException, GroundControlException cause constructor
- Entity accessor coverage for Requirement and RequirementRelation (toString, getDescription, setDescription, getWave, getCreatedAt, getUpdatedAt)

### Fixed

- CI: `gradle-wrapper.jar` was excluded by `*.jar` gitignore rule overriding the earlier negation — reordered rules so the negation comes after `*.jar` and uses `**/` glob to match `backend/` path
- SonarQube S125: converted JML `// @` annotations to `/*@ ... @*/` block comment syntax and added `@SuppressWarnings("java:S125")` to classes with JML contracts (Requirement, RequirementRelation, RequirementService, Status)
- SonarQube S1948: `DomainValidationException.detail` field changed from `Map<String, Object>` to `Map<String, Serializable>` (exception is Serializable)
- SonarQube S2187: suppressed false positive on `RequirementTest` (tests are in `@Nested` inner classes)
- SpotBugs EI_EXPOSE_REP: `DomainValidationException.getDetail()` now returns defensive copy via `Map.copyOf()`

## [0.21.1] - 2026-03-09

### Changed

- Inner dev loop optimized: `make rapid` (format + compile, ~1s warm) for edit-compile cycles
- Added `-Pquick` Gradle property to disable Error Prone, SpotBugs, and Checkstyle for fast iteration
- Added `rapid` Gradle task (format + compile, no tests or static analysis)
- Pre-commit: switched `spotlessCheck --no-daemon` to `spotlessApply` (auto-fix), upgraded test hook to full `./gradlew check` (CI-equivalent), dropped `--no-daemon` to keep daemon warm
- Makefile: added `rapid`, `check`, `integration`, `verify` targets; `build`/`test` use `-Pquick`
- CLAUDE.md: `make rapid` is now the primary inner loop command
- CODING_STANDARDS.md: pre-alpha workflow step 4 uses `make rapid`; Git & CI section documents pre-commit runs full check

## [0.21.0] - 2026-03-09

### Added

- ADR-014: Pluggable Verification Architecture — separates internal dogfooding (JML/OpenJML) from platform verification capabilities (polyglot, multi-prover). Introduces VerificationResult domain entity, TLA+ for design-level verification, and verifier adapter pattern
- TLA+ adopted for design-level verification of state machines, DAG invariants, and materialization consistency

### Changed

- ADR-011: Updated from Django to Java/Spring Boot implementation details (per ADR-013). Core decisions unchanged. TraceabilityLink artifact types now include TLA+ specs and verification results
- ADR-002: Updated from Django ORM/psycopg/django-tenants to Hibernate/Spring Data JPA/Flyway
- ADR-012: Reframed assurance levels as universal methodology (not JML-specific). Added TLA+ at L2. Added ADR-014 reference. **Default assurance level lowered to L0 for pre-alpha** — contracts only on state transitions and security boundaries, one test per behavior, no two-tests-per-contract requirement. Full L1-default SDD workflow deferred to beta
- Phase 1 design notes rewritten for Java: JPA entities, Spring services, JML contracts, EnumMap state machine, Envers auditing, command records
- CODING_STANDARDS.md: Pre-alpha workflow (implementation-first, contracts where they prevent silent corruption, one test per behavior). Coverage threshold stays at 30%. Post-alpha targets documented
- CLAUDE.md: Added pre-alpha development philosophy

## [0.20.0] - 2026-03-09

### Added

- Flyway migrations V003-V005: Envers audit tables (`revinfo`, `requirement_audit`, `requirement_relation_audit`)
- `RequirementService` with 9 methods: create, getById, getByUid, update, transitionStatus, archive, createRelation, getRelations, list. JML contracts on state-transition methods (L1: transitionStatus, archive, createRelation); retained as documentation on CRUD methods (L0)
- `CreateRequirementCommand` and `UpdateRequirementCommand` records
- `RequirementController` REST controller with 9 endpoints under `/api/v1/requirements`
- API DTOs: `RequirementRequest`, `RequirementResponse`, `StatusTransitionRequest`, `RelationRequest`, `RelationResponse`
- `MethodArgumentNotValidException` handler in `GlobalExceptionHandler` for Jakarta Bean Validation errors (422)
- OpenJML ESC integration: `gradle/openjml.gradle.kts` with `downloadOpenJml`, `openjmlEsc`, `openjmlRac` tasks — verifies state machine contracts via Z3 solver
- SpotBugs static analysis with exclusions for JPA entities, test code, and constructor-throw patterns
- Error Prone compiler plugin for additional compile-time bug detection
- Checkstyle for naming conventions and coding patterns (complements Spotless formatting)
- JaCoCo coverage verification thresholds wired into `check` task
- Testcontainers base class (`BaseIntegrationTest`) with singleton PostgreSQL 16 container
- `MigrationSmokeTest`: verifies Flyway V001-V005 ran, audit tables exist, Hibernate validates
- `RequirementServiceIntegrationTest`: 7 tests covering CRUD, Envers audit trail, conflict/validation errors
- `RequirementControllerIntegrationTest`: 13 MockMvc tests covering all endpoints, error envelopes (404/409/422)
- `RequirementServiceTest`: 20 Mockito unit tests (happy-path + violation for all 9 service contracts)
- ArchUnit rules: controllers must not access repositories, controllers must not import entities, services must reside in `..service..` packages
- OpenJML ESC Scoping section in CODING_STANDARDS.md with design guidelines for ESC-verifiable code
- CI: `integration` job (Testcontainers, no external DB service), `verify` job (OpenJML ESC)
- SonarCloud workflow updated to run both unit and integration tests for combined coverage

### Changed

- Exception hierarchy moved from `domain/requirements/exception/` to `domain/exception/` (shared across all domain areas)
- CI workflow: removed standalone `architecture` job (ArchUnit runs as part of `check`), removed external Postgres service (Testcontainers manages its own)
- Testcontainers upgraded from 1.20.4 to 1.21.1 (Docker 29+ API version compatibility)
- ADR-012 "Tool Integration" section updated with actual OpenJML commands, scope limitations, and known issues

## [0.19.0] - 2026-03-08

### Added

- ADR-013: Java/Spring Boot Backend Rewrite — documents pivot from Python 3.12/Django to Java 21/Spring Boot 3.4 with JML/OpenJML contracts, jqwik property testing, and KeY formal proofs
- ADR-012: Formal Methods Development Process — Specification-Driven Development (SDD) methodology with assurance levels L0-L3, updated for Java toolchain
- Java 21 / Spring Boot 3.4 / Gradle (Kotlin DSL) project scaffold in `backend/`
- `Requirement` and `RequirementRelation` JPA entities with JML contract annotations and Hibernate Envers auditing
- `Status` enum with hand-rolled `EnumMap` transition table (DRAFT -> ACTIVE -> DEPRECATED -> ARCHIVED)
- `RequirementType`, `Priority`, `RelationType` domain enums
- Exception hierarchy: `GroundControlException` base with `NotFoundException`, `DomainValidationException`, `AuthenticationException`, `AuthorizationException`, `ConflictException`
- `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping exceptions to `{"error": {...}}` JSON envelope
- `RequestLoggingFilter` for MDC `request_id` binding
- Spring Data JPA repositories for `Requirement` and `RequirementRelation`
- Flyway migrations V001 (requirement table) and V002 (requirement_relation table)
- `logback-spring.xml` with console (dev) and JSON/Logstash (prod) output
- 22 tests: JUnit 5 unit tests (13), jqwik property tests (3), ArchUnit architecture rules (4), structural transition table tests (5), smoke test (1)
- ArchUnit rules enforcing `api/ -> domain/ <- infrastructure/` dependency rule
- Spotless + Palantir Java Format for code formatting
- JaCoCo for test coverage reporting
- Springdoc-OpenAPI for API documentation generation

### Changed

- Backend rewritten from Python 3.12/Django to Java 21/Spring Boot 3.4 with Gradle (Kotlin DSL)
- ADR-001 (Django backend), ADR-003 (icontract), ADR-004 (Python toolchain) marked as superseded by ADR-013
- ADR-012 tool references updated: icontract → JML, CrossHair → OpenJML ESC, Hypothesis → jqwik, Rocq/Coq → KeY
- CI workflow rewritten for Gradle (build, test, architecture jobs)
- Makefile updated for Gradle commands
- Dockerfile rewritten as multi-stage JDK 21 build
- CODING_STANDARDS.md rewritten for Java conventions
- CLAUDE.md updated with Java build commands

### Removed

- Python backend code (pyproject.toml, Django settings, manage.py, all Python source)

## [0.16.0] - 2026-03-08

### Added

- ADR-011: Requirements data model — documents UUID PKs, DAG relations, AGE-as-query-layer strategy, service-layer write ownership, no new library dependencies
- Phase 1 design notes (`architecture/notes/phase1-requirements-design.md`) with data model, app structure, service layer architecture, and key patterns
- Design documentation index (`architecture/design/README.md`)

## [0.15.0] - 2026-03-08

### Added

- `backend/Dockerfile` multi-stage build with non-root user (closes #161)
- `backend/.dockerignore` excluding tests, dev files, .venv
- `make docker-build` target for local image builds
- GitHub Actions `docker.yml` workflow for GHCR publishing on push to main/tags
- `gunicorn` production dependency
- `STATIC_ROOT` setting for collectstatic support

## [0.14.0] - 2026-03-08

### Added

- `BaseSchema` base class for all project schemas (closes #164)
- `GroundControlPagination` with `PageMeta` for consistent paginated responses
- Nested error response format: `{"error": {"code": ..., "message": ..., "detail": ...}}`
- Schemas & Response Format section in CODING_STANDARDS.md

### Changed

- Error responses now use nested `{"error": {...}}` format (breaking API change, no consumers)
- Replaced `ErrorResponse` schema with `ErrorDetail` + `ErrorEnvelope`

## [0.13.0] - 2026-03-08

### Added

- Shared exception hierarchy in `ground_control.exceptions` (closes #163)
- `GroundControlError` base with `NotFoundError`, `DomainValidationError`, `AuthenticationError`, `AuthorizationError`, `ConflictError`
- django-ninja exception handler mapping domain exceptions to HTTP status codes
- `ErrorResponse` Pydantic schema for structured API error responses

### Changed

- Moved `NinjaAPI` instance from `urls.py` to `ground_control.api` for cleaner separation

## [0.12.0] - 2026-03-08

### Added

- Structured logging with structlog and django-structlog (closes #162)
- JSON log output in production, colored console in development (based on DEBUG)
- Automatic request context binding (request_id, ip, user_id) via django-structlog middleware
- Service identity fields (service.name, service.version) in all log entries
- Standard library logging routed through structlog for unified output

### Removed

- Custom `RequestIdMiddleware` (replaced by django-structlog's `RequestMiddleware`)

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
