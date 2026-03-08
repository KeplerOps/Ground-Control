# ADR-011: Requirements Data Model

## Status

Accepted

## Date

2026-03-08

## Context

Ground Control needs a requirements management system to replace the archived StrictDoc tool and issue-graph tool with in-app functionality. This is the first domain code — the domain layer (`backend/src/ground_control/domain/`) is currently empty.

The system must:

- Model requirements with human-readable UIDs, parent/child DAG relations, and traceability to external artifacts (GitHub issues, code files, ADRs, configs, tests)
- Sync with GitHub issues for cross-referencing and label-based metadata
- Support graph traversal for impact analysis and coverage gap detection
- Import existing requirements from the archived StrictDoc `project.sdoc` file (~145 requirements)

Key constraints:

- Requirements form a **DAG** (directed acyclic graph), not a tree — a requirement can have multiple parents
- The system must dogfood itself: Ground Control manages its own requirements
- Apache AGE (graph extension for PostgreSQL) is available for optimized traversal queries

## Decision

### 1. UUID Primary Keys

All domain models use `UUIDField` primary keys (`default=uuid4`). This is consistent with the archived DATA_MODEL.md and is better for API exposure (no sequential ID enumeration) and distributed systems (no central sequence coordination).

### 2. Django App Structure

Create `ground_control.domain.requirements` as a standalone Django app with its own `models.py`, `admin.py`, `apps.py`, and `choices.py`. Future domain areas (risks, controls, assessments) follow the same pattern, keeping concerns cleanly separated under `ground_control.domain.`*.

### 3. DAG Relations via Through-Model (No django-mptt/treebeard)

Requirements form a DAG, not a tree. Tree libraries like django-mptt and django-treebeard cannot model multiple parents. Instead, a `RequirementRelation` through-model stores typed edges (parent, depends_on, refines, conflicts, supersedes, related) with a unique constraint on `(source, target, relation_type)`.

### 4. Simple Status Machine (No django-fsm)

Requirement states (draft, active, deprecated, archived) are a simple linear progression with few valid transitions. `CharField` with `icontract` preconditions is sufficient — django-fsm's overhead is not justified for 4 states.

### 5. Audit via django-auditlog (No django-simple-history)

`django-auditlog` is already installed and wired up (see ADR-001). All new models register with it. No reason to add a second audit library.

### 6. AGE as Query Layer, ORM as Source of Truth

All CRUD operations go through Django ORM models. Apache AGE is a **read-only query layer** materialized from ORM data:

- A `sync_age_graph` management command materializes `Requirement` nodes and typed relation edges
- Graph traversal queries (impact analysis, dependency chains, path finding) use Cypher via raw SQL
- Core analysis functions (cycle detection, orphan detection, coverage gaps) also work via ORM for environments without AGE
- This avoids dual-write consistency issues and keeps AGE optional

### 7. GitHub Sync via `gh` CLI (No Webhooks)

Batch import of GitHub issues via a management command that shells out to `gh api`. This reuses parsing logic from the archived `issue_graph.py` tool. No new Python dependencies, no webhook infrastructure. Sufficient for the dogfooding use case.

### 8. Service Layer Write Ownership

All five models live in one Django app, but mutations are governed by service-layer ownership: each model has exactly one service that may write to it. Other services may read via ORM querysets but never mutate directly. This gives us decoupling where it matters (mutation paths, testability, reasoning about side effects) without the overhead of splitting into multiple Django apps prematurely.

- `RequirementService` owns `Requirement` and `RequirementRelation`
- `TraceabilityService` owns `TraceabilityLink`
- `SyncService` owns `GitHubIssueSync`
- `ImportService` owns `RequirementImport` and orchestrates cross-service import flows
- `AnalysisService` is read-only across all models

Management commands and API views act as orchestrators — they call services in sequence but services do not call each other horizontally. No Django signals for cross-service communication.

See [Phase 1 design notes](../notes/phase1-requirements-design.md#service-layer-architecture) for the full rationale and rules.

### Data Model

Five models in `ground_control.domain.requirements`:


| Model                   | Purpose                                  | Key Fields                                                                                                          |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **Requirement**         | Core requirement record                  | uid (unique), title, statement, type, priority (MoSCoW), status, wave, tags (ArrayField), custom_fields (JSONField) |
| **RequirementRelation** | DAG edges between requirements           | source FK, target FK, relation_type, unique_together(source, target, relation_type)                                 |
| **TraceabilityLink**    | Links requirements to external artifacts | requirement FK, artifact_type, artifact_identifier, link_type, sync_status                                          |
| **GitHubIssueSync**     | Cached GitHub issue data                 | issue_number (unique), labels (JSON), phase, priority_label, cross_references (JSON)                                |
| **RequirementImport**   | Audit trail for bulk imports             | source_type, stats (JSON), errors (JSON)                                                                            |


## Consequences

### Positive

- DAG model is more expressive than a tree — accurately represents real requirement relationships
- UUID PKs are safe for API exposure and future multi-tenant distribution
- AGE-as-query-layer avoids consistency issues while enabling powerful graph queries
- No new library dependencies — simpler dependency tree, fewer version conflicts
- Django app structure scales cleanly to future domain areas
- StrictDoc import preserves all existing requirement data
- Service-layer write ownership prevents mutation spaghetti without premature app splitting

### Negative

- DAG cycle detection must be implemented manually (no library enforcement)
- AGE sync adds a materialization step that can drift if not run after ORM changes
- `gh` CLI batch sync means GitHub data is always slightly stale (no real-time webhooks)

### Risks

- AGE extension availability varies across PostgreSQL hosting providers (mitigated: core analysis works without AGE via ORM)
- StrictDoc format may evolve or have edge cases not covered by the parser (mitigated: import is a one-time migration, not ongoing)
- DAG traversal via ORM adjacency list is O(depth * branching_factor) per query (mitigated: AGE provides O(1)-ish traversal for production use)
