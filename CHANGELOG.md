# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.78.0] - 2026-03-24

### Added

- Configurable quality gates for CI/CD integration (`POST /api/v1/quality-gates`,
  `POST /api/v1/quality-gates/evaluate`) with per-project pass/fail thresholds
  for coverage, orphan count, and completeness metrics
- Quality gate evaluation integrated into analysis sweep reports
- MCP tools: `gc_create_quality_gate`, `gc_list_quality_gates`,
  `gc_get_quality_gate`, `gc_update_quality_gate`, `gc_delete_quality_gate`,
  `gc_evaluate_quality_gates`

## [0.77.0] - 2026-03-24

### Added

- Structured requirement version diff API (`GET /api/v1/requirements/{id}/diff`)
  returning per-field changes, added/removed/modified relations, and
  added/removed/modified traceability links between two revision numbers
- MCP tool `gc_get_requirement_diff` for agent-driven change review workflows

## [0.76.0] - 2026-03-24

### Added

- Project-wide audit timeline endpoint (`GET /api/v1/audit/timeline`) aggregating
  changes across all requirements in a project
- Actor filtering on requirement and project audit timelines
- CSV export of audit timeline for compliance reporting
  (`GET /api/v1/audit/timeline/export`)
- Optional `reason` field on status transitions, recorded in the audit trail
  for governance traceability
- Configurable audit data retention with scheduled cleanup
  (`GC_AUDIT_RETENTION_DAYS`, disabled by default)
- MCP tools: `gc_get_project_timeline`, `gc_export_audit_timeline`
- MCP tools `gc_transition_status` and `gc_bulk_transition_status` now accept
  optional `reason` parameter

## [0.75.2] - 2026-03-23

### Fixed

- Add `latest` Docker tag for default branch pushes in CI workflow so EC2 deploy
  can pull `ground-control:latest` from ECR
- Capture both stdout and stderr from SSM deploy command on failure for proper
  diagnostics (previously only stderr was shown, hiding docker logs)
- Increase deploy health check timeout from 60s to 10min, SSM command timeout
  from 120s to 660s, and CI wait loop to match
- Fix V014 Flyway migration failing on prod data with case-duplicate UIDs
  (`OBS-001` / `obs-001`): drop unique constraint before normalizing to
  uppercase, and deduplicate colliding rows by renaming with `-DUP-N` suffix

## [0.75.1] - 2026-03-22

### Fixed

- Fix `RequirementService.getRelations()` mutating unmodifiable JPA result list
  by creating a mutable copy before combining outgoing and incoming relations
  (closes #321)

## [0.75.0] - 2026-03-22

### Changed

- Replace `List<Map<String, Object>>` error fields in API responses with typed records
  (GC-A012 tech debt, closes #324):
  - `BulkStatusTransitionResponse.failed` now uses `BulkFailureDetail(id, uid, error)`
  - `ImportResultResponse.errors` now uses `ImportError(phase, uid, error, parent, target, issueRef)`
  - `SyncResultResponse.errors` now uses `SyncError(phase, issue, artifactIdentifier, error)`
- Domain result records `BulkTransitionResult`, `ImportResult`, `SyncResult` updated accordingly
- Audit JSONB persistence unchanged — `RequirementImport.errors` column remains untyped JSON

## [0.74.4] - 2026-03-23

### Fixed

- Paginate `GitHubCliClient.fetchAllIssues()` using GitHub REST API to fetch all
  issues instead of silently truncating at 500; filters out pull requests and
  normalizes state values (closes #317)

## [0.74.3] - 2026-03-23

### Changed

- Replaced N+1 in-memory filtering in `AnalysisService.findOrphans()` and
  `findCoverageGaps()` with single JPQL `NOT EXISTS` queries in
  `RequirementRepository` (closes #318)

## [0.74.2] - 2026-03-23

### Fixed

- Added exception handlers for `HttpMessageNotReadableException`,
  `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`,
  `MissingServletRequestPartException`, and generic `Exception` catch-all to ensure
  all API errors return the consistent `ErrorResponse` envelope (closes #315)

## [0.74.1] - 2026-03-23

### Added

- JML contract annotations on `TraceabilityLink` domain model: class invariants,
  constructor pre/postconditions, and setter contracts for `setSyncStatus`,
  `setArtifactUrl`, and `setArtifactTitle` (closes #327)

## [0.74.0] - 2026-03-22

### Added

- Semantic similarity detection (GC-C016): pairwise cosine similarity analysis
  across requirement embeddings, returning overlap candidates above a configurable
  threshold sorted by similarity score
- REST API endpoint: `GET /api/v1/analysis/semantic-similarity?threshold=0.85&project=...`
- MCP tool: `gc_analyze_similarity` with optional threshold and project parameters
- Configurable default threshold via `GC_EMBEDDING_SIMILARITY_THRESHOLD` (default 0.85)
- Auto-re-embed on requirement text change: updating title, statement, or rationale
  automatically triggers re-embedding after the transaction commits (fire-and-forget,
  gracefully skipped when no provider is configured)

## [0.73.0] - 2026-03-22

### Added

- Requirement text embedding infrastructure (GC-C015): pluggable vector embedding
  of requirement text content (title, statement, rationale) with content-hash-based
  staleness detection and batch embedding support
- `EmbeddingProvider` domain interface with `NoOpEmbeddingProvider` (default, graceful
  degradation) and `OpenAiEmbeddingProvider` (conditional on `GC_EMBEDDING_PROVIDER=openai`)
- REST API endpoints: `POST /api/v1/embeddings/{id}`, `GET /api/v1/embeddings/{id}/status`,
  `POST /api/v1/embeddings/batch`, `DELETE /api/v1/embeddings/{id}`
- MCP tools: `gc_embed_requirement`, `gc_get_embedding_status`, `gc_embed_project`
- Flyway migration V015 creates `requirement_embedding` table with BYTEA storage,
  SHA-256 content hash, and model tracking
- Configuration via `GC_EMBEDDING_PROVIDER`, `GC_EMBEDDING_API_KEY`,
  `GC_EMBEDDING_MODEL`, `GC_EMBEDDING_DIMENSIONS`, `GC_EMBEDDING_BATCH_SIZE`

## [0.72.0] - 2026-03-22

### Fixed

- Requirement UID uniqueness is now case-insensitive per project: `OBS-001` and
  `obs-001` can no longer coexist as separate requirements
- Flyway migration V014 normalizes existing UIDs to uppercase and replaces the
  composite unique constraint with a functional index on `LOWER(uid)`
- Service layer normalizes UIDs to uppercase on create and clone
- All UID lookups (create, clone, import, GitHub sync, getByUid) use
  case-insensitive matching

## [0.71.0] - 2026-03-22

### Added

- Subgraph extraction endpoint `GET /api/v1/graph/subgraph?roots=UID1,UID2` (GC-G003):
  given a set of root requirements, returns all transitively reachable requirements and
  their relations as a self-contained graph
- MCP tool `gc_extract_subgraph` for API/MCP parity

## [0.70.0] - 2026-03-21

### Changed

- Path finding endpoint `GET /api/v1/graph/paths` now returns structured response
  with nodes and edges (including relation types) instead of flat UID arrays (GC-G002)

## [0.69.0] - 2026-03-21

### Added

- Unified graph visualization endpoint `GET /api/v1/graph/visualization` (GC-G005):
  returns all requirement nodes and relation edges in a single response
- MCP tool `gc_get_graph_visualization` for API/MCP parity

### Changed

- Frontend graph page fetches all data in a single API call instead of N+1
  separate requests (paginated requirements + per-requirement relation fetches)

## [0.68.0] - 2026-03-21

### Added

- Interactive dependency graph enhancements (GC-Q005): explicit filter controls
  for status, priority, series, and wave that remove non-matching nodes from
  the graph layout (distinct from legend click-to-filter visual highlighting)
- Wave-ordered DAG layout modes (`dagre-wave-tb`, `dagre-wave-lr`) that group
  nodes by wave number while preserving dagre's edge-based ordering
- Frontend test infrastructure: vitest with `make frontend-test` target
- Unit tests for graph-constants module (`getSeries`, `getNodeColor`, `getColorMap`)

### Removed

- Standalone roadmap-viewer prototype (`tools/roadmap-viewer/`) and its nginx
  service from docker-compose — superseded by the React graph page

## [0.67.0] - 2026-03-21

### Added

- Audit history timeline (GC-Q006): unified timeline endpoint
  `GET /api/v1/requirements/{id}/timeline` that merges requirement, relation,
  and traceability link changes into a single chronologically-sorted view with
  field-level diffs between consecutive revisions
- Timeline supports filtering by change category (REQUIREMENT, RELATION,
  TRACEABILITY_LINK) and date range (from/to)
- MCP tool `gc_get_timeline` for querying the unified audit timeline
- Frontend timeline UI on the requirement detail History tab with visual
  vertical timeline, expandable field-level diff views, and filter controls

## [0.66.0] - 2026-03-21

### Added

- Scheduled analysis sweeps (GC-C013): configurable cron-based execution of the
  full analysis suite (orphan detection, coverage gaps, cross-wave validation,
  cycle detection, consistency checks) with GitHub issue and webhook notification
  support for detected problems
- REST API endpoints: `POST /api/v1/analysis/sweep` (single project) and
  `POST /api/v1/analysis/sweep/all` (all projects) for manual sweep triggering
- MCP tool `gc_run_sweep` for triggering analysis sweeps via MCP
- Configurable notification channels: GitHub issue creation and webhook POST
  for sweep results with problems

### Fixed

- GitHub CLI (`gh`) not found by Java backend: now auto-resolves the binary
  path from common locations (`/usr/bin/gh`, `/usr/local/bin/gh`,
  `/opt/homebrew/bin/gh`) and supports explicit override via `GC_GH_PATH`
  environment variable

## [0.65.0] - 2026-03-20

### Added

- Slide-out detail panel on the requirements explorer: clicking a table row
  opens an inline panel showing requirement details, status transitions, and
  editing — without navigating away from the list
- Reusable `SlidePanel` UI component (right-edge drawer with slide animation)
- Selected row highlight in requirements table when detail panel is open

## [0.64.1] - 2026-03-20

### Fixed

- Project switcher now preserves query string (e.g., `?status=ACTIVE`) when
  switching projects
- Unknown routes (`/p/:projectId/bad-page`, `/random-path`) now render a
  "Page not found" message instead of a blank page

## [0.64.0] - 2026-03-20

### Changed

- Move project identity from localStorage into URL path (`/p/:projectId/...`),
  making the URL the single source of truth for project context
- Route structure changed: project-scoped pages now live under `/p/:projectId/`
  (e.g., `/p/my-project/requirements`); `/projects` stays at root level
- `ProjectProvider` now derives `activeProject` from `useParams()` instead of
  localStorage; `setActiveProject` navigates to the new project URL
- Project switcher preserves the current sub-path when switching projects
- Projects page uses `useProjects()` directly instead of `useProjectContext()`
- Root `/` redirects to `/p/<first-project>/`; invalid project IDs redirect to
  `/projects` with a toast
- All `navigate()` and `Link` paths updated to use project-prefixed URLs across
  dashboard, requirements, requirement detail, and analysis pages

### Removed

- localStorage-based project persistence (`gc-active-project` key)

## [0.63.2] - 2026-03-20

### Changed

- Refactor `ImportService`: extract shared helpers (`upsertRequirements`, `createParentRelations`,
  `resolveRequirementId`, `createExplicitRelations`, `createTraceabilityLinks`, `saveAuditAndBuildResult`)
  to reduce cognitive complexity and duplication between `importStrictdoc` and `importReqif`
- Extract `ParsedRequirement` record and `ImportCounters` accumulator as shared types
- Extract `ATTR_IDENTIFIER` and `ATTR_LONG_NAME` string constants in `ReqifParser`

### Added

- 7 new tests for uncovered reqif relation paths (DB fallback, missing parent/source/target,
  creation errors for hierarchy and explicit relations)
- `@SuppressWarnings("java:S2187")` on `ReqifParserTest` to suppress false positive

### Fixed

- Fix SonarCloud S1751 bug in `ReqifParser.extractAttrValueText` — unconditional `return` inside
  `for` loop replaced with explicit first-element check

## [0.63.1] - 2026-03-20

### Fixed

- Remove dead `relations` field from `ReqifRequirement` record (was never populated)
- Remove redundant null check in `extractAttrValueText` (`getAttribute` never returns null)
- Make `stripXhtml` method private (only used internally)
- Add comment clarifying hierarchy + SpecRelation overlap behavior in Phase 2b

### Added

- Test: title fallback from `LONG-NAME` to `ReqIF.Name` attribute value
- Test: hierarchy + explicit SpecRelation overlap correctly skips duplicate

## [0.63.0] - 2026-03-20

### Added

- ReqIF 1.2 import — bulk-import requirements from `.reqif` files produced by
  enterprise tools (IBM DOORS, Polarion, Jama)
- REST API: `POST /admin/import/reqif` (multipart/form-data)
- MCP tool: `gc_import_reqif` with `file_path` and optional `project` parameters
- Parses SPEC-OBJECTS (title, statement), SPEC-RELATIONS (explicit relations),
  and SPECIFICATION hierarchy (parent-child nesting)
- XHTML attribute values stripped to plain text
- XXE prevention: DTDs and external entities disabled
- Relation type mapping from ReqIF type names via naming convention
  (contains "parent" → PARENT, "depends" → DEPENDS_ON, etc.)
- Deterministic UID truncation for identifiers exceeding 50 characters

## [0.62.1] - 2026-03-20

### Fixed

- Update integration test migration assertions to include V013 (`create_baseline`)

## [0.62.0] - 2026-03-20

### Added

- Baseline management — named point-in-time snapshots of the requirement set
  for release management, audit trails, and specification evolution tracking
- REST API: `POST/GET /baselines`, `GET /baselines/{id}`,
  `GET /baselines/{id}/snapshot`, `GET /baselines/{id}/compare/{otherId}`,
  `DELETE /baselines/{id}`
- MCP tools: `gc_create_baseline`, `gc_list_baselines`, `gc_get_baseline`,
  `gc_get_baseline_snapshot`, `gc_compare_baselines`, `gc_delete_baseline`
- Baseline snapshots reconstruct requirements via Hibernate Envers
  `forEntitiesAtRevision()`, filtered by project and non-archived status
- Baseline comparison diffs two snapshots showing added, removed, and modified
  requirements with before/after detail

## [0.61.1] - 2026-03-20

### Changed

- Move `SATISFIED_STATUSES` and `PRIORITY_ORDER` static fields to top of
  `AnalysisService` alongside other static constants

### Added

- Test: cycle participants appended sorted by priority in work order
- Test: cross-wave dependencies excluded from intra-wave topological sort

## [0.61.0] - 2026-03-19

### Added

- Work order API (`GET /api/v1/analysis/work-order`) — topologically-sorted,
  DAG-derived work order grouped by wave, with MoSCoW priority tie-breaking
- `gc_get_work_order` MCP tool with REST/MCP parity
- `GraphAlgorithms.topologicalSort()` — Kahn's algorithm with priority
  tie-breaking for deterministic ordering
- Blocking status detection: each requirement classified as UNBLOCKED, BLOCKED,
  or UNCONSTRAINED based on dependency satisfaction

## [0.60.0] - 2026-03-18

### Added

- AWS EC2 deployment infrastructure — single `t3a.small` instance running
  Docker Compose with Tailscale-only access (zero public ingress) (ADR-018)
- Terraform `compute` module: EC2 instance, IAM instance profile, EBS data
  volume with cloud-init bootstrapping (Docker, Tailscale, compose)
- Terraform `backup` module: S3 bucket for pg_dump backups (30-day lifecycle),
  DLM policy for daily EBS snapshots (7-day retention)
- ECR container registry for deployment images — EC2 pulls via IAM role (no
  tokens needed), CI pushes to both GHCR and ECR
- Production Docker Compose (`deploy/docker/docker-compose.prod.yml`) — ECR
  image, EBS bind mounts, no Redis, JVM memory caps
- Automated deployment: CI pushes to `main` trigger deploy to EC2 via SSM
  `SendCommand` after smoke test passes — no manual SSH needed
- Operational scripts: `backup.sh` (pg_dump + S3), `watchdog.sh` (health check
  + auto-restart), `deploy.sh` (pull + restart + verify)
- Makefile targets: `deploy` (SSH deploy to EC2), `deploy-infra` (terraform
  apply)
- ADR-018: AWS EC2 Deployment — documents architecture, cost, and rationale

### Changed

- Terraform `networking` module rewritten for zero-ingress security group
  (Tailscale-only, was CIDR-based ingress for RDS)
- Terraform `secrets` module rewritten for Tailscale auth key + DB password
  (was RDS host/user/pass)
- Terraform `environments/dev` rewritten to wire compute + networking + backup
  + secrets modules (was RDS + networking + secrets)
- Bootstrap IAM policy updated: replaced RDS permissions with EC2, IAM instance
  profile, S3 backup bucket, DLM, SSM SendCommand, and ECR permissions
- CI workflow (`ci.yml`): added `deploy` job that auto-deploys to EC2 on
  push to `main`, added `id-token: write` permission for OIDC, added ECR
  push alongside GHCR
- Deployment docs updated with AWS deployment section

### Fixed

- TypeScript build errors in `requirements.tsx` — added explicit types for
  `BulkStatusTransitionResponse` callback and `RequirementResponse` map parameter
- Docker build failure: `.gitignore` pattern `lib/` was excluding
  `frontend/src/lib/` (api-client, query-client, utils) from git — changed
  to `/lib/` so it only matches the repo-root Python dist directory

### Removed

- Terraform `rds` module (stale — RDS withdrawn per ADR-015)

## [0.58.0] - 2026-03-18

### Added

- Project health dashboard endpoint `GET /api/v1/analysis/dashboard-stats` —
  returns aggregate metrics: requirement counts by status and wave, traceability
  coverage percentages per link type, and recent changes (GC-Q004)
- `gc_dashboard_stats` MCP tool for dashboard stats retrieval
- Enriched frontend dashboard with wave progress bars, traceability coverage
  percentages, recent changes feed, and clickable stat cards linking to detail
  views

## [0.57.1] - 2026-03-18

### Fixed

- CI Docker build and smoke test used `context: backend/` but the Dockerfile
  references repo-root paths (`backend/`, `frontend/`) — changed to
  `context: .` with `file: backend/Dockerfile` to match docker-compose.yml

## [0.57.0] - 2026-03-18

### Added

- Completeness analysis backend endpoint `GET /api/v1/analysis/completeness` —
  returns total count, status distribution, and missing-field issues (GC-C008)
- `gc_analyze_completeness` MCP tool now calls the backend API instead of doing
  client-side computation, achieving full REST/MCP parity for all 7 analysis
  operations
- Completeness tab on the frontend analysis page
- Coverage-gaps integration test

## [0.56.0] - 2026-03-18

### Added

- Consistency violation detection analysis — detects ACTIVE requirements linked
  by `CONFLICTS_WITH` relations and `SUPERSEDES` relations where both sides are
  ACTIVE (GC-C007)
- REST endpoint `GET /api/v1/analysis/consistency-violations`
- `gc_analyze_consistency` MCP tool
- Consistency tab on the frontend analysis page

## [0.55.0] - 2026-03-18

### Added

- `sort` parameter on `gc_list_requirements` MCP tool, achieving parity with
  the REST API's `sort` query parameter (e.g. `sort: "uid,asc"`)

## [0.54.2] - 2026-03-18

### Fixed

- Graph view zoom/scroll speed was too slow (wheelSensitivity 0.3 → 1)

## [0.54.1] - 2026-03-18

### Fixed

- Cross-wave validation logic was inverted — flagged valid "later depends on
  earlier" relationships instead of invalid "earlier depends on later" ones
- All analysis endpoints (cycles, orphans, coverage gaps, cross-wave) now
  exclude archived requirements

## [0.54.0] - 2026-03-17

### Added

- **Full-parity frontend** — every REST API capability is now accessible via the
  UI, covering all 34 endpoints across 7 controllers
- **Requirements list page** — paginated table with filtering (status, type,
  priority, wave, free-text search), column sorting, bulk status transitions via
  checkbox selection, per-row status dropdown for quick transitions, and create
  modal
- **Requirement detail page** with tabbed interface:
  - Details tab: view/edit all fields, status transitions, clone, archive
  - Relations tab: list, add, delete relations with search-based target picker
  - Traceability tab: list, add, delete traceability links to external artifacts
  - History tab: audit timeline showing all revisions with snapshots
  - Impact tab: transitive impact analysis for the requirement
- **Dashboard rewrite** — project health overview with requirement counts by
  status and clickable analysis alert cards (cycles, orphans, coverage gaps,
  cross-wave violations)
- **Analysis page** — tabbed view for dependency cycles, orphan requirements,
  coverage gaps (by link type), and cross-wave violations
- **Projects page** — list, create, edit, and switch projects
- **Admin page** — StrictDoc import (file upload), GitHub sync, GitHub issue
  creation, and graph materialization
- Shared type definitions (`src/types/api.ts`) for all API request/response types
- `apiDelete()` and `apiUpload()` utilities in the API client
- React Query hooks: `use-requirements`, `use-relations`, `use-traceability`,
  `use-analysis`, `use-history`
- Reusable UI components: Modal, Badge (status/priority/type), FormField,
  ConfirmDialog, Toast notifications, StatusBadgeDropdown, RequirementForm,
  RelationForm, TraceabilityForm
- Navigation expanded: Dashboard, Requirements, Graph, Analysis, Projects, Admin
- Radix UI dependencies: dialog, tabs, dropdown-menu, toast, checkbox

## [0.53.0] - 2026-03-16

### Added

- **Web application shell** (GC-Q008): Bootstrap React 19 frontend with Vite 6,
  TanStack Query 5, React Router 7, and Tailwind CSS 4
- **Project switcher** in app header — persistent project selection via
  localStorage, auto-selects when only one project exists, hidden when
  single-project
- **Interactive graph view** — full port of the roadmap viewer into the React
  app with Cytoscape.js, dagre layout, color-by (series/priority/status/wave),
  legend filtering, node click highlighting, tooltips, fit/reset controls
- Dashboard page showing active project details
- Requirements page with project-scoped requirement listing
- `SpaController` — Spring Boot controller forwarding non-API routes to
  `index.html` for client-side routing
- Typed `apiFetch<T>()` wrapper with automatic `?project=` injection from
  context
- Multi-stage Docker build: Node frontend stage → Spring Boot backend stage
- Makefile targets: `frontend-install`, `frontend-dev`, `frontend-build`,
  `frontend-lint`, `frontend-format`
- Biome for frontend formatting and linting

### Changed

- Docker build context changed from `backend/` to project root (`.`) with
  explicit `dockerfile: backend/Dockerfile`
- `docker-build` Make target updated for new build context

## [0.52.0] - 2026-03-16

### Added

- **Project scoping** (GC-A013): Ground Control now supports multiple independent
  projects within a single instance. All requirements, relations, and analysis are
  scoped to a project.
- `POST /api/v1/projects` — create a new project
- `GET /api/v1/projects` — list all projects
- `GET /api/v1/projects/{identifier}` — get project by identifier
- `PUT /api/v1/projects/{identifier}` — update project name/description
- Optional `project` query parameter on requirement, analysis, graph, import, and
  GitHub issue endpoints. When omitted and only one project exists, auto-resolves
  to that project. When multiple projects exist and param is missing, returns 422.
- `gc_list_projects` and `gc_create_project` MCP tools
- Optional `project` parameter on 17 existing MCP tools for project-scoped operations
- `project_identifier` field in all requirement API responses
- Same-project validation: relations can only be created between requirements in
  the same project
- Flyway migration V012: creates `project` table, inserts default "ground-control"
  project, adds `project_id` to requirements with composite unique constraint
  `(project_id, uid)`
- Composite indexes on `(project_id)`, `(project_id, status)`, `(project_id, uid)`
- `gc_analyze_completeness` MCP tool for requirement completeness analysis

## [0.51.0] - 2026-03-15

### Changed

- **Breaking:** `PUT /api/v1/requirements/{id}` now uses `UpdateRequirementRequest`
  DTO — all fields are optional for partial updates, `uid` removed from request body

### Added

- Interactive graph screenshot in README showing DAG layout and requirement details

### Fixed

- Omitting `wave` in a requirement update request no longer resets it to null
- `gc_update_requirement` MCP tool no longer accepts `uid` parameter (UID updates
  were silently ignored); partial updates now work correctly without 422 errors

## [0.50.0] - 2026-03-15

### Changed

- **Breaking:** `GET /api/v1/analysis/cycles` now returns objects with `members`
  and `edges` fields instead of plain UID arrays. Each edge includes `sourceUid`,
  `targetUid`, and `relationType`, fulfilling GC-C001 requirement to report
  which relation types form each cycle (GC-C001)

## [0.49.0] - 2026-03-15

### Added

- 9 new MCP tools for full REST/MCP feature parity (GC-A012):
  `gc_get_requirement_history`, `gc_get_relation_history`,
  `gc_get_traceability_link_history`, `gc_delete_relation`,
  `gc_delete_traceability_link`, `gc_materialize_graph`, `gc_get_ancestors`,
  `gc_get_descendants`, `gc_find_paths`
- REST endpoint `POST /api/v1/admin/github/issues` to create GitHub issues from
  requirements, with automatic traceability link creation
- `GitHubClient.createIssue()` domain interface method and `GitHubCliClient`
  implementation using `gh issue create` CLI
- `CreateGitHubIssueCommand` and `CreateGitHubIssueResult` domain records
- `GitHubIssueSyncService.createIssueFromRequirement()` orchestrates issue
  creation, body formatting from requirement metadata, and traceability link
  creation with graceful degradation on link failure
- `GitHubIssueController`, `GitHubIssueRequest`, and `GitHubIssueResponse` API
  layer types
- Unit tests for controller and service; URL parsing test for `GitHubCliClient`

### Changed

- `gc_create_github_issue` MCP tool now delegates to the backend REST API
  instead of shelling out to `gh` CLI directly

## [0.48.0] - 2026-03-15

### Added

- Actor identity population via `X-Actor` HTTP header on every request
  (`ActorFilter`); defaults to "anonymous" when header is absent (GC-P002)
- Audit history API for relations: `GET /api/v1/requirements/{id}/relations/{relationId}/history`
- Audit history API for traceability links: `GET /api/v1/requirements/{id}/traceability/{linkId}/history`
- MCP server now sends `X-Actor: mcp-server` header on all API requests
- Unit tests for `ActorFilter`, relation history endpoint, and traceability
  link history endpoint
- Integration tests for actor identity recording, relation history, and
  traceability link history

### Changed

- JaCoCo line coverage threshold raised from 30% to 80% to match SonarCloud
  quality gate

## [0.47.0] - 2026-03-15

### Added

- Audit history REST endpoint: `GET /api/v1/requirements/{id}/history` returns
  chronological list of all revisions with revision type, timestamp, actor, and
  full entity snapshot at each point in time (GC-A006)
- Custom Envers revision entity (`GroundControlRevisionEntity`) with `actor`
  column for tracking who made each change (nullable until auth is added)
- `ActorHolder` thread-local utility for propagating actor identity to Envers
- `AuditService` for querying Hibernate Envers revision history
- Flyway migration V011: adds `actor` column to `revinfo` table
- Integration test for audit history endpoint (create + update + verify history)

## [0.46.1] - 2026-03-15

### Added

- Integration tests for `RequirementSpecifications`: all 6 spec methods and
  `fromFilter()` branches tested against real PostgreSQL (19 tests)
- Unit tests for `RequirementService` uncovered branches: create with null
  optional fields, update with all-null fields, update rationale (3 tests)
- JaCoCo report now merges unit + integration test coverage data

### Changed

- `RequirementSpecifications.java` coverage: 54.2% line / 55.0% branch -> 100% / 100%
- `RequirementService.java` coverage: 99.0% line / 77.8% branch -> 100% / 100%
- Overall project coverage: 89.7% line / 78.3% branch -> 94.2% / 86.8%

## [0.46.0] - 2026-03-15

### Added

- Priority filtering for requirements list endpoint: `GET /api/v1/requirements?priority=MUST`
  supports MoSCoW values (MUST, SHOULD, COULD, WONT). Completes GC-A009
- `priority` parameter in `gc_list_requirements` MCP tool

## [0.45.1] - 2026-03-15

### Fixed

- Docker roadmap viewer 403 Forbidden when files change: mount stable parent
  directory (`tools/roadmap-viewer`) at `/srv/roadmap` instead of mounting
  subdirectories whose inodes change on git operations

## [0.45.0] - 2026-03-15

### Fixed

- Cypher injection bug in `escapeCypher()`: backslash escaping now runs before
  quote escaping, preventing malformed output like `O\\'Malley` (unescaped quote)
- `Requirement.archive()` no longer double-sets `archivedAt` — the assignment in
  `transitionStatus(ARCHIVED)` is the single canonical source
- `ImportController.importStrictdoc()` wraps `IOException` in `GroundControlException`
  instead of leaking it outside the error envelope

### Changed

- Narrowed `catch(Exception)` blocks to specific exception types in `ImportService`
  (3 blocks → `ConflictException | NotFoundException | DomainValidationException`)
  and `GitHubIssueSyncService` (2 blocks → `RuntimeException`)
- Standardized 8 log messages across `ImportService`, `GitHubIssueSyncService`,
  `GitHubCliClient`, and `AgeGraphService` to semantic event names
  (e.g. `import_requirement_failed:`, `graph_materialized:`)
- Added `@SuppressWarnings("java:S125")` to `Status.java` for block JML annotations
- Removed redundant `@Transactional` from `RequirementService.bulkTransitionStatus()`
  (already covered by class-level annotation)

## [0.44.0] - 2026-03-15

### Added

- Requirement cloning: `POST /api/v1/requirements/{id}/clone` creates a new
  requirement by copying content fields (title, statement, rationale, type,
  priority, wave) with a new UID in DRAFT status, optionally copying outgoing
  relations. Implements GC-A007
- `gc_clone_requirement` MCP tool for cloning requirements by UID

## [0.43.0] - 2026-03-14

### Added

- Bulk status transitions: `POST /api/v1/requirements/bulk/transition` accepts
  a list of requirement IDs and a target status, applies the same state machine
  rules to each independently (best-effort semantics — valid transitions succeed,
  invalid ones collected as failures). Implements GC-A008
- `BulkTransitionResult` domain record, `BulkStatusTransitionRequest` and
  `BulkStatusTransitionResponse` API DTOs
- `gc_bulk_transition_status` MCP tool: accepts UIDs, resolves to UUIDs, calls
  the bulk endpoint, merges UID-resolution errors into the failure list
- Unit tests for `RequirementService.bulkTransitionStatus()` (3 tests) and
  `RequirementController.bulkTransitionStatus()` (2 tests)

## [0.42.1] - 2026-03-14

### Changed

- ADR-017: Split graph visualization into two libraries — React Flow for
  structured local neighborhood views (requirement detail page), Sigma.js +
  Graphology for force-directed whole-graph exploration (`/graph` route).
  Replaces single React Flow approach that was wrong for organic exploration
  of 50–500 node graphs

## [0.42.0] - 2026-03-14

### Added

- Interactive roadmap viewer — Cytoscape.js + dagre DAG visualization of the
  full requirement graph, served as a containerized nginx static site
  (implements GC-Q005)
- Color coding switchable between series, priority, status, and wave dimensions
- Node selection with neighborhood highlighting and click-to-deselect
- Edge legend showing actual line styles (solid/dashed/dotted) for relation types
- CORS configuration for dev profile (`CorsConfig`, `@Profile("dev")`)
- Backend and roadmap services added to `docker-compose.yml`
- ADR-017 updated with Cytoscape.js prototype implementation notes

## [0.41.0] - 2026-03-13

### Added

- Duplicate relation pre-check in `RequirementService.createRelation()` — returns
  a clean `ConflictException` instead of letting the DB unique constraint produce
  an unhandled SQL exception (completes GC-A004 service-layer enforcement)
- Unit test for duplicate relation rejection (`throwsConflictForDuplicateRelation`)
- Integration test for duplicate relation rejection end-to-end
  (`duplicateRelationThrowsConflict`)

## [0.40.0] - 2026-03-13

### Added

- `SUPERSEDES` and `RELATED` relation types, completing all 6 typed DAG
  relations specified by GC-A003 (PARENT, DEPENDS_ON, CONFLICTS_WITH, REFINES,
  SUPERSEDES, RELATED). Both are non-DAG types — they do not participate in
  cycle detection or impact analysis
- Unit tests for SUPERSEDES and RELATED relation creation

## [0.39.0] - 2026-03-13

### Added

- `gc_create_github_issue` MCP tool: creates a GitHub issue from a requirement
  (via `gh` CLI), formats the issue body with requirement metadata, and
  auto-creates an IMPLEMENTS traceability link — single command replaces the
  manual copy-fields → `gh issue create` → `gc_create_traceability_link` workflow
- `formatIssueBody` and `createGitHubIssue` library functions in MCP server
- `GH_REPO` env var in `.mcp.json` for default GitHub repository target

### Changed

- `README.md` rewritten for current implemented state: features, getting
  started, tech stack, architecture, documentation index, project status

## [0.38.0] - 2026-03-13

### Added

- GC-Q001–Q006: new User Interface domain (6 requirements) — Interactive Web
  Application, Requirements Explorer, Traceability Matrix, Project Health
  Dashboard, Interactive Dependency Graph, Audit History Timeline
- ADR-016: Project Scoping — architectural decisions for multi-project support
  (Project entity, same-project relation constraint, project-scoped operations,
  UID uniqueness scope change)
- ADR-017: Interactive Web Application — technology decisions (React 19 +
  TypeScript + Vite SPA, embedded in Spring Boot, TanStack Query/Table,
  React Flow for dependency graph, shadcn/ui components)

## [0.37.0] - 2026-03-13

### Added

- GC-A013 (Project Scoping) — new wave 1 requirement for multi-project support
  via a Project entity with single-project scoped operations

### Changed

- GC-A002 (Status State Machine) activated with full traceability: 7 IMPLEMENTS,
  4 TESTS, 1 DOCUMENTS links
- Fixed `gc_transition_status` MCP tool description to include missing
  ACTIVE->ARCHIVED transition

## [0.36.0] - 2026-03-13

### Added

- `gc_get_relations` MCP tool for inspecting a requirement's incoming and
  outgoing relations through the MCP interface

### Changed

- GC-E004 (Link Health Tracking) promoted from wave 3 to wave 2 — staleness
  detection must ship alongside the features that create traceability links
- GC-E005 (Artifact Change Detection) promoted from wave 3 to wave 2 — same
  rationale; without change detection, every refactor silently degrades link quality

## [0.35.1] - 2026-03-13

### Fixed

- Wired 9 orphaned requirements into the dependency graph: GC-C002→GC-A001,
  GC-D003→GC-D001, GC-D004→GC-D001, GC-I004→GC-I003, GC-M002→GC-M001,
  GC-N001→GC-A006 (REFINES), GC-N002→GC-N001, GC-P005→GC-F005, GC-P007→GC-B001.
  Orphan count reduced from 13 to 4 (the remaining 4 are accepted cross-cutting
  concerns: Authentication, Full-Text Search, Notification System, Multi-Tenancy)

## [0.35.0] - 2026-03-13

### Added

- `mcp/ground-control/README.md`: MCP server usage documentation — setup,
  workflow order of operations, tool reference table, enum values, error format
- `docs/API.md`: REST API reference — endpoint tables, filtering, pagination,
  error envelope, interactive docs pointers

### Changed

- Requirement listing excludes archived requirements by default (`archivedAt IS NULL`);
  filtering by `status=ARCHIVED` still returns them explicitly
- `RequirementService.list()` always uses specification path (no more bypass
  for null filter)
- Removed stale `docs/requirements/infrastructure.sdoc` (described withdrawn
  RDS/Terraform infrastructure)
- `docker-compose.yml`: database password read from `.env` instead of hardcoded;
  ports bound to `127.0.0.1` (not `0.0.0.0`)
- `Makefile`: `dev` target sources `.env` before running Spring Boot

## [0.34.2] - 2026-03-13

### Fixed

- `docs/deployment/DEPLOYMENT.md`: rewrote from stale Python 3.12/Django/uv/gunicorn
  content to actual Java 21/Spring Boot 3.4/Gradle stack
- `.github/PULL_REQUEST_TEMPLATE.md`: replaced `mypy --strict`/`tsc`/`ruff check`/`biome`
  references with `make check` (Spotless, SpotBugs, Error Prone, JaCoCo); removed
  non-existent tenant isolation checklist item
- `.github/ISSUE_TEMPLATE/bug_report.md`: removed Kubernetes deployment option and
  browser field (no frontend exists)
- `README.md`: replaced aspirational "verification-aware lifecycle orchestrator" tagline
  with accurate "requirements management system with traceability and graph analysis";
  rewrote "What is Ground Control?" to describe actual current functionality; removed
  Redis from Quick Start comment
- `docs/architecture/ARCHITECTURE.md`: restructured "What Exists" into categorized
  sections (entities, services, API, tooling); expanded "Does not exist yet" to include
  frontend, Redis integration, production deployment, auth, multi-tenancy, AGE
  optional degradation caveat

## [0.34.1] - 2026-03-13

### Fixed

- `archivedAt` timestamp now set when transitioning to ARCHIVED via
  `/transition` endpoint (previously only set via `/archive` endpoint)
- `LazyInitializationException` on `GET /api/v1/requirements/{id}/relations`
  — relation queries now use fetch joins to eagerly load source/target
  requirement entities

## [0.34.0] - 2026-03-13

### Added

- `make smoke`: local smoke test — builds Docker image, runs against fresh
  PostgreSQL 16, verifies Flyway migrations and health endpoint

### Removed

- RDS infrastructure destroyed — ADR-015 withdrawn because RDS does not
  support Apache AGE, violating ADR-005's single-database commitment
- Cloud DB Makefile targets (`cloud-db-env`, `dev-cloud`, `cloud-db-ip`)
- CI terraform job, path detection job, OIDC permissions, workflow_dispatch
  terraform inputs

### Changed

- ADR-015 status changed from Accepted to Withdrawn
- CI pipeline simplified: `build → test → integration/verify → docker → smoke`
  (no terraform dependency)
- Development defaults to local Docker Compose with `apache/age` image;
  named volume `gc-postgres-data` provides data durability across rebuilds

### Fixed

- CI: `docker/build-push-action` SHA had a single-character typo (`d` → `e`)
  causing the docker job to fail; updated to v6.19.2

## [0.33.0] - 2026-03-12

### Added

- Terraform dev environment wiring (`deploy/terraform/environments/dev/`):
  S3 backend configuration, provider setup, module composition (networking →
  RDS → secrets), and developer-facing outputs (RDS endpoint, SSM paths)
- `terraform.tfvars.example` documenting required variables for dev environment
- CI smoke test job: builds Docker image, runs against fresh PostgreSQL 16,
  verifies Flyway migrations apply and health endpoint returns UP

### Changed

- Unified CI/CD pipeline: all jobs flow through a single dependency chain
  (`build → test → integration → docker → smoke`)
- Docker images now built on `dev` branch pushes (in addition to `main`/tags)
- SonarCloud is now non-blocking (`continue-on-error: true`); reports quality metrics
  without gating merges

### Fixed

- CI race conditions: concurrency group prevents parallel runs on the same branch,
  cancels in-progress PR runs on new pushes
- CI waste: integration tests now depend on unit tests passing (`needs: [build, test]`)
- Bootstrap S3 lifecycle rule missing required `filter {}` block (future provider error)
- RDS module: removed `manage_master_user_password = false` (conflicts with `password` in AWS provider ~>5.0)
- `.gitignore`: added `tfplan` pattern for extensionless Terraform plan files; removed accidentally committed binary plan file

## [0.32.0] - 2026-03-12

### Added

- Terraform networking module (`deploy/terraform/modules/networking/`): security group
  with configurable ingress CIDR for database access, default VPC lookup
- Terraform RDS module (`deploy/terraform/modules/rds/`): PostgreSQL 16 on db.t4g.micro,
  gp3 storage, forced SSL via parameter group, encryption at rest, deletion protection,
  7-day backup retention, random password generation
- Terraform secrets module (`deploy/terraform/modules/secrets/`): SSM Parameter Store
  entries for database host, username, and password (SecureString)

### Changed

- Bootstrap IAM role policy expanded with EC2, RDS, and SSM permissions for
  Terraform CI to plan and apply infrastructure modules

## [0.31.0] - 2026-03-12

### Added

- Terraform bootstrap (`deploy/terraform/bootstrap/`): S3 state bucket with versioning,
  encryption, and public access blocking; DynamoDB lock table for state locking;
  GitHub Actions OIDC identity provider and IAM role
- Terraform CI workflow (`.github/workflows/terraform.yml`): `terraform fmt`, `validate`,
  and `plan` on PRs to `deploy/terraform/**`; manual `apply` via `workflow_dispatch`;
  AWS authentication via OIDC federation
- ADR-015 updated with Terraform CI/CD sub-decision
- Pre-commit hooks for Terraform: `terraform_fmt`, `terraform_validate`
  (antonbabenko/pre-commit-terraform), and Checkov IaC security scanning

## [0.30.0] - 2026-03-12

### Added

- Infrastructure requirements (`docs/requirements/infrastructure.sdoc`): 10 requirements
  across cloud database, infrastructure as code, and developer workflow sections
- ADR-015: Cloud Database Deployment — RDS PostgreSQL 16 in catalyst-dev (us-east-2),
  SSM for credentials, Terraform for IaC, accepts AGE unavailability per ADR-005
- Phase 2 design notes (`architecture/notes/phase2-cloud-database-design.md`): topology,
  RDS configuration, security model, Terraform structure, cost estimate, migration paths
- 6 GitHub issues for cloud database implementation (Terraform bootstrap, modules,
  environment wiring, Makefile targets, data migration, .gitignore)

## [0.29.0] - 2026-03-12

### Added

- Ground Control MCP server (`mcp/ground-control/`): 18 tools wrapping the REST API
  for native Claude Code integration — requirements CRUD, analysis, StrictDoc import,
  GitHub sync, and traceability link management

## [0.28.0] - 2026-03-12

### Added

- `RequirementsE2EIntegrationTest`: end-to-end integration test verifying all Phase 1
  components — migration, StrictDoc import, GitHub sync, CRUD API, analysis, and
  Envers audit trail (6 ordered test steps)
- `RequirementsE2EAgeIntegrationTest`: optional AGE E2E test verifying graph
  materialization and Cypher queries match JPA analysis (`@Tag("age")`)
- Test fixture `test-requirements.sdoc` (5 requirements, 2 parent relations,
  5 GitHub issue references, 2 waves)

### Changed

- Phase 1 complete: all acceptance criteria verified end-to-end

## [0.27.0] - 2026-03-12

### Added

- `GraphAlgorithms` pure utility class with `findCycles()` (DFS three-color) and `findReachable()` (BFS) — JML contracts, no Spring dependencies (L2)
- `AnalysisService` read-only service: cycle detection, orphan detection, coverage gap analysis, transitive impact analysis, cross-wave validation
- `GraphClient` domain port interface for graph traversal operations (ancestors, descendants, path finding)
- `AgeGraphService` infrastructure adapter (`@Component`): Apache AGE graph materialization and Cypher queries, optional via `groundcontrol.age.enabled`
- `AgeConfig` + `AgeProperties` configuration for AGE integration
- `AnalysisController` REST endpoints: `GET /api/v1/analysis/{cycles,orphans,coverage-gaps,impact/{id},cross-wave}`
- `GraphController` REST endpoints: `POST /api/v1/admin/graph/materialize`, `GET /api/v1/graph/{ancestors,descendants,paths}`
- `RequirementSummaryResponse` and `RelationValidationResponse` API DTOs
- V010 Flyway migration: Apache AGE graph setup with graceful fallback on plain PostgreSQL
- Unit tests: `AnalysisServiceTest` (14 tests), `AgeGraphServiceTest` (4 tests), `AnalysisControllerTest` (6 tests), `GraphControllerTest` (4 tests)
- Property tests (L2): `CycleDetectionPropertyTest` (4 properties), `ImpactAnalysisPropertyTest` (3 properties) — jqwik
- Integration tests: `AnalysisIntegrationTest` (4 tests)
- AGE integration tests: `BaseAgeIntegrationTest`, `AgeGraphServiceIntegrationTest` (3 tests) — `@Tag("age")`, separate `ageTest` Gradle task

### Changed

- `RequirementRelationRepository`: added `findAllWithSourceAndTargetByRelationTypeIn()` and `findAllWithSourceAndTarget()` with JOIN FETCH for N+1 prevention
- `TraceabilityLinkRepository`: added `existsByRequirementId()` and `existsByRequirementIdAndLinkType()` for analysis queries
- `build.gradle.kts`: added `ageTest` task, excluded `@Tag("age")` from `test` and `integrationTest` tasks
- `application.yml`: added `groundcontrol.age.*` configuration properties
- `MigrationSmokeTest`: updated expected migration count to include V010

## [0.26.0] - 2026-03-12

### Added

- `GitHubIssueData` domain record for fetched GitHub issue data
- `GitHubClient` domain port interface for GitHub issue fetching
- `SyncResult` record for sync operation results with statistics
- `GitHubIssueSyncService` with `syncGitHubIssues()` method: fetches issues via `GitHubClient`, upserts `GitHubIssueSync` records with parsed labels/phase/priority/cross-references, updates `TraceabilityLink` records with synced metadata, saves audit records
- `GitHubCliClient` infrastructure adapter (`@Component`): executes `gh issue list` CLI, parses JSON output into `GitHubIssueData` records (first class in `infrastructure/` package)
- `SyncController` REST endpoint: `POST /api/v1/admin/sync/github?owner=X&repo=Y`
- `SyncResultResponse` API DTO with `static from()` factory
- Unit tests: `GitHubIssueSyncServiceTest` (9 tests), `GitHubCliClientTest` (3 tests), `SyncControllerTest` (3 tests)
- Integration tests: `SyncIntegrationTest` (idempotent sync, creates issue sync records, updates traceability links)

### Changed

- `TraceabilityLinkRepository`: added `findByArtifactType()` for bulk traceability link updates during sync
- SpotBugs exclusions: added `EI_EXPOSE_REP2` exclusion for `infrastructure` package

## [0.25.0] - 2026-03-12

### Added

- `SdocParser` pure Java utility for parsing StrictDoc (.sdoc) requirement files, ported from Python reference implementation
- `SdocRequirement` record for parsed requirement data (UID, title, statement, comment, issue refs, parent UIDs, wave)
- `ImportResult` record for import operation results with full statistics
- `ImportService` with idempotent `importStrictdoc()` method: upserts requirements, creates relations, creates traceability links, saves audit records
- `ImportController` REST endpoint: `POST /api/v1/admin/import/strictdoc` (multipart file upload)
- `ImportResultResponse` API DTO with `static from()` factory
- Unit tests: `SdocParserTest` (8 tests), `ImportServiceTest` (8 tests), `ImportControllerTest` (2 tests)
- Integration tests: `ImportIntegrationTest` (idempotent import, creates requirements/relations/links)

### Changed

- `RequirementRelationRepository`: added `existsBySourceIdAndTargetIdAndRelationType()` for idempotent relation creation
- `TraceabilityLinkRepository`: added `existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType()` for idempotent link creation

## [0.24.0] - 2026-03-12

### Added

- `TraceabilityService` with `createLink`, `getLinksForRequirement`, `deleteLink` methods
- `TraceabilityLinkRequest` and `TraceabilityLinkResponse` API DTOs
- `CreateTraceabilityLinkCommand` and `RequirementFilter` domain records
- `RequirementSpecifications` utility for dynamic JPA Criteria filtering
- REST endpoints: `POST/GET/DELETE /{id}/traceability` for traceability link CRUD
- REST endpoint: `DELETE /{id}/relations/{relationId}` for relation deletion (204)
- Filtered requirement listing via `GET /api/v1/requirements?status=X&type=X&wave=N&search=text`
- `TraceabilityServiceTest` unit tests (create, get, delete links)
- `TraceabilityLinkControllerIntegrationTest` integration tests (CRUD + 404)
- Unit and integration tests for filtered listing and relation deletion

### Changed

- `RequirementRepository` now extends `JpaSpecificationExecutor<Requirement>` for dynamic filtering
- `RequirementService.list()` accepts `RequirementFilter` parameter for filtered/searchable listing
- `RequirementController` accepts `TraceabilityService` as second constructor dependency

## [0.23.0] - 2026-03-11

### Added

- `TraceabilityLink` JPA entity with `@Audited`, `@ManyToOne` FK to `Requirement`, unique constraint on `(requirement_id, artifact_type, artifact_identifier, link_type)`, and sync status tracking
- `GitHubIssueSync` JPA entity with JSONB fields (`issueLabels`, `crossReferences`) for GitHub issue caching
- `RequirementImport` JPA entity with JSONB fields (`stats`, `errors`) for import audit trails
- `ArtifactType`, `LinkType`, `SyncStatus`, `IssueState`, `ImportSourceType` domain enums
- `TraceabilityLinkRepository`, `GitHubIssueSyncRepository`, `RequirementImportRepository` Spring Data repositories
- Flyway migrations V006-V009: `traceability_link`, `github_issue_sync`, `requirement_import` tables and `traceability_link_audit` Envers table
- Unit tests for all three new entities (defaults, construction, accessors)
- Integration tests for FK persistence, JSONB round-trip, Envers audit trail verification, and migration smoke test coverage through V009

### Changed

- ADR-011 Section 5: clarified Envers auditing applies to business entities only, not cache tables or self-auditing records

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
- All JML annotations converted from `// @` (invalid, never parsed by OpenJML) to `/*@ ... @*/` block comment syntax (valid JML). Added `@SuppressWarnings("java:S125")` to out-of-ESC-scope classes with JML contracts
- Status.java: replaced `EnumMap` transition table with `switch` expression for OpenJML ESC compatibility (EnumMap specs incomplete in OpenJML). Added `/*@ pure @*/` on both methods, `/*@ ensures \result != null @*/` on `validTargets()`, `/*@ requires target != null @*/` on `canTransitionTo()`. All contracts verified by Z3
- CODING_STANDARDS.md: updated JML section to document `/*@ @*/` block syntax, inline modifiers (`pure`, `spec_public`), and SonarQube S125 suppression; updated Git & CI section to document pre-commit ESC hook
- Pre-commit: added `openjml-esc` hook to run OpenJML ESC verification on `domain/requirements/state/` files at commit time
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
