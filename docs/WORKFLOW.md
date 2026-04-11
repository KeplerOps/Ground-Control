# Ground Control Workflow

How to use Ground Control as a requirements-driven development platform — from idea to shipped, traceable, auditable software.

## Philosophy

Ground Control treats every artifact in the software lifecycle as a node in a graph: requirements, code files, tests, ADRs, operational assets, verification results. Every relationship is an edge. The graph is the single source of truth — no spreadsheets, no disconnected tools, no traceability theater.

The workflow is designed for AI-assisted development. Requirements are authored collaboratively with an AI agent via MCP, implementation is driven by requirement UIDs, and traceability links close the loop automatically. The platform doesn't just track what you built — it tracks *why* you built it, *what* proves it works, and *what breaks* if it changes.

## The Graph

```
Requirement ──PARENT──► Requirement
     │                       │
     ├──DEPENDS_ON──►        ├──IMPLEMENTS──► Code File
     ├──CONFLICTS_WITH──►    ├──TESTS──► Test File
     ├──REFINES──►           ├──DOCUMENTS──► ADR
     ├──SUPERSEDES──►        ├──CONSTRAINS──► Config/Policy
     └──RELATED──►           └──VERIFIES──► Proof/Spec
                                    │
                             Operational Asset
                              ├──CONTAINS──►
                              ├──DEPENDS_ON──►
                              ├──COMMUNICATES_WITH──►
                              └──TRUST_BOUNDARY──►
```

Every node has a lifecycle. Every edge has a type. Every change is versioned. One Cypher query can traverse the entire graph.

## Phase 1: Define Requirements

### Create a Project

Every requirement lives in a project. Projects scope all operations — analysis, baselines, quality gates, documents.

```
gc_create_project(identifier: "my-system", name: "My System")
```

### Author Requirements

Requirements are the atomic unit. Each has a human-readable UID (e.g., `GC-R001`), a title, a formal statement, optional rationale, type (FUNCTIONAL, NON_FUNCTIONAL, CONSTRAINT, INTERFACE), MoSCoW priority (MUST, SHOULD, COULD, WONT), and a wave number for release planning.

```
gc_create_requirement(
  uid: "GC-R001",
  title: "User Authentication",
  statement: "The system shall authenticate users via OAuth2 before granting access to protected resources.",
  requirement_type: "FUNCTIONAL",
  priority: "MUST",
  wave: 1
)
```

Requirements start in DRAFT status. The lifecycle is forward-only:

```
DRAFT → ACTIVE → DEPRECATED → ARCHIVED
```

Transition to ACTIVE when the requirement is reviewed and approved. Every transition is recorded in the audit trail with timestamp, actor, and optional reason.

### Import Existing Specs

Already have requirements? Import them:

- **StrictDoc (.sdoc):** `gc_import_strictdoc` — imports documents, sections, text blocks, requirements, and relations. Idempotent by UID.
- **ReqIF 1.2 (.reqif):** `gc_import_reqif` — standard interchange format compatible with IBM DOORS, Polarion, Jama. Idempotent by IDENTIFIER.

### Build the Requirements Graph

Connect requirements with directed relations to model dependencies, hierarchy, and conflicts:

| Relation | Meaning |
|----------|---------|
| `PARENT` | Hierarchical containment — parent covers child |
| `DEPENDS_ON` | Blocking dependency — child can't be done until parent is done |
| `CONFLICTS_WITH` | Mutual exclusion — both cannot be active simultaneously |
| `REFINES` | Elaboration — child is a concrete version of abstract parent |
| `SUPERSEDES` | Replacement — new requirement replaces old |
| `RELATED` | Weak association for reference |

```
gc_create_relation(
  source_id: <req-uuid>,
  target_id: <other-req-uuid>,
  relation_type: "DEPENDS_ON"
)
```

### Validate the Graph

Before committing to implementation, run analysis to catch structural problems:

- **`gc_analyze_cycles`** — Circular dependencies that block delivery
- **`gc_analyze_orphans`** — Requirements disconnected from the graph
- **`gc_analyze_cross_wave`** — Wave 2 requirements depending on Wave 3 (ordering violation)
- **`gc_analyze_consistency`** — Active requirements linked by CONFLICTS_WITH
- **`gc_analyze_completeness`** — Missing fields, status distribution
- **`gc_run_sweep`** — All of the above in one call

Or run `gc_dashboard_stats` for an aggregate health view: counts by status and wave, coverage percentages, and recent change activity.

### Find Duplicates

If you have a large requirement set, use semantic similarity to find near-duplicates:

```
gc_embed_project(project: "my-system")   # Generate embeddings
gc_analyze_similarity(threshold: 0.85)    # Find similar pairs
```

Merge or refactor duplicates before they become parallel implementations.

## Phase 2: Plan & Prioritize

### Wave-Based Planning

Requirements are grouped into waves (release increments). Wave 1 ships first, Wave 2 second, etc. MoSCoW priority ranks within a wave.

`gc_get_work_order` returns a topologically sorted queue: requirements ordered by wave, then by MoSCoW priority, with blocked requirements (unresolved dependencies) flagged.

### Organize into Documents

For stakeholder communication, organize requirements into narrative specifications:

```
Document: "System Requirements Specification v1.0"
  ├── Section: "1. Introduction"
  │     └── Text: "This document defines..."
  ├── Section: "2. Authentication"
  │     ├── Requirement: GC-R001 (User Authentication)
  │     ├── Requirement: GC-R002 (Session Management)
  │     └── Text: "OAuth2 was selected because..."
  └── Section: "3. Data Storage"
        └── Requirement: GC-R003 (Encryption at Rest)
```

Documents support arbitrary nesting, mixed content (requirement references + prose), optional grammars (custom fields, allowed types), and export to StrictDoc, HTML, PDF, or ReqIF.

### Set Quality Gates

Define pass/fail thresholds that requirements must meet before code ships:

```
gc_create_quality_gate(
  name: "Test Coverage",
  metric_type: "COVERAGE",
  metric_param: "TESTS",
  operator: "GTE",
  threshold: 80,
  scope_status: "ACTIVE"
)
```

Available metrics: `COVERAGE` (% of requirements with a link type), `ORPHAN_COUNT`, `COMPLETENESS`. Gates evaluate in CI via `gc_evaluate_quality_gates`.

For this repository, the human-maintained policy entrypoints are:

- `make policy` for repo-native guardrails shared by Claude and Codex
- `make sync-ground-control-policy` to sync ADR metadata and quality-gate definitions into Ground Control
- `make policy-live` to validate live Ground Control gates and non-regression sweep baselines when a reachable GC instance is available

## Phase 3: Implement

### The Development Loop

Pick the next unblocked requirement from the work order and implement it. Ground Control's `/implement` skill automates the entire cycle:

- The current repository's `AGENTS.md` should define repo-local Ground Control context using a `Ground Control Context` section with a fenced YAML block.
- The `/implement` skill validates this up front via `gc_get_repo_ground_control_context` and stops rather than guessing if the repo context is missing or invalid.
- The `/implement` argument should be the full requirement UID as it already exists in Ground Control.

1. **Fetch requirement** from Ground Control
2. **Create GitHub issue** and link it via traceability
3. **Checkout feature branch** via `gh issue develop`
4. **Plan implementation** — user reviews and approves
5. **Write code, tests, docs** — clause-by-clause verification against the requirement statement
6. **Create traceability links:**
   - `IMPLEMENTS` → code files that satisfy the requirement
   - `TESTS` → test files that verify the requirement
   - `DOCUMENTS` → ADRs or design docs that explain the approach
7. **Transition to ACTIVE** once implemented and verified

Before you stop, run `make policy` alongside the feature-specific verification commands. This catches ADR drift, missing controller/MCP/doc parity, migration companion updates, and PR body omissions before review.

### Record Architectural Decisions

When implementation involves a design choice, create an ADR:

```
gc_create_adr(
  uid: "ADR-018",
  title: "AWS EC2 Deployment",
  decision_date: "2026-03-15",
  context: "Need a deployment target for the application",
  decision: "Deploy to AWS EC2 with Docker",
  consequences: "Simple, cost-effective, single-instance"
)
```

ADRs have their own lifecycle (PROPOSED → ACCEPTED → DEPRECATED | SUPERSEDED) and link to the requirements they impact via traceability. `gc_get_adr_requirements` shows reverse traceability: given an ADR, which requirements does it affect?

### Model Operational Assets

For systems with infrastructure concerns, model the operational landscape:

```
gc_create_asset(
  uid: "ASSET-001",
  name: "API Gateway",
  asset_type: "SERVICE",
  description: "NGINX reverse proxy"
)
```

Asset types: APPLICATION, SERVICE, DATABASE, NETWORK, HOST, CONTAINER, IDENTITY, DATA_STORE, BOUNDARY. Assets connect via relations: CONTAINS, DEPENDS_ON, COMMUNICATES_WITH, TRUST_BOUNDARY, SUPPORTS, ACCESSES, DATA_FLOW.

Asset topology supports cycle detection and impact analysis — "what services are affected if this database goes down?"

## Phase 4: Verify & Ship

### Quality Gate Evaluation

Before merging, evaluate all quality gates:

```
gc_evaluate_quality_gates(project: "my-system")
```

Returns overall pass/fail + per-gate details (actual value vs. threshold). Fix failures: write missing tests, link orphaned requirements, clean up duplicates.

### Architecture + Review Pipeline

The `/implement` skill now runs one mandatory Codex architecture preflight before coding and then four independent verification/review stages before the PR is presented for human review:

1. **Codex architecture preflight** — cross-cutting concerns, reuse opportunities, abstraction/concept confusion, ADR/design guidance when needed
2. **SonarCloud** — static analysis, coverage, duplication, security hotspots
3. **Codex (ChatGPT)** — exhaustive no-triage review for design, abstractions, maintainability, reliability, security, and consistency
4. **Claude /review** — code quality, conventions, correctness, performance
5. **Claude /security-review** — OWASP Top 10, injection, auth, data exposure

All findings are fixed before the PR is presented for human review.

### Impact Analysis

Before shipping, understand the blast radius:

```
gc_analyze_impact(uid: "GC-R001")
```

Returns all transitively affected requirements — everything upstream and downstream that could be impacted by a change to this requirement.

## Phase 5: Release & Audit

### Create a Baseline

Freeze the requirement set at a release milestone:

```
gc_create_baseline(name: "v1.0", project: "my-system")
```

Baselines capture the Envers revision at creation time. `gc_get_baseline_snapshot` reconstructs the full requirement set as it existed at that point — without maintaining separate copies.

### Compare Releases

```
gc_compare_baselines(baseline_id: <v1>, other_baseline_id: <v2>)
```

Shows added, removed, and modified requirements between two baselines. Essential for release notes, change impact assessment, and regulatory audits.

### Audit Trail

Every change to every entity is versioned:

- `gc_get_requirement_history` — all revisions with timestamps and actors
- `gc_get_requirement_diff` — structured diff between two revisions (per-field changes)
- `gc_get_timeline` — unified chronological stream of all changes to a requirement
- `gc_get_project_timeline` — same, across all requirements in a project
- `gc_export_audit_timeline` — CSV export for compliance reporting

### Export for Stakeholders

- **Traceability matrix:** `gc_export_requirements` (Excel with traceability sheet)
- **Specification document:** `gc_export_document` (PDF, HTML, StrictDoc, ReqIF)
- **Quality report:** `gc_export_sweep_report` (CSV/Excel/PDF with analysis results)
- **Audit log:** `gc_export_audit_timeline` (CSV)

## Graph Queries

The graph enables queries that cross-cut the entire lifecycle:

| Question | How to answer |
|----------|--------------|
| What breaks if this requirement changes? | `gc_analyze_impact` |
| Which requirements have no tests? | `gc_analyze_coverage_gaps(link_type: "TESTS")` |
| Are there circular dependencies? | `gc_analyze_cycles` |
| What's the full dependency chain from A to Z? | `gc_find_paths(source, target)` |
| Show me everything related to feature X | `gc_extract_subgraph(root_uids: [...])` |
| What changed between v1.0 and v2.0? | `gc_compare_baselines` |
| Who changed this and when? | `gc_get_timeline` |
| Are there near-duplicate requirements? | `gc_analyze_similarity` |
| Which ADRs affect this requirement? | Traceability links with artifact_type=ADR |
| Which requirements does this ADR affect? | `gc_get_adr_requirements` |
| What assets are impacted if this service goes down? | Asset impact analysis |

## MCP Integration

Ground Control exposes its full API as MCP tools. This means an AI agent (Claude Code, Cursor, etc.) can:

- Query requirements and their graph relationships
- Create and manage traceability links as it writes code
- Run analysis to validate the requirement graph
- Create GitHub issues and sync traceability
- Transition requirement statuses as part of the implementation workflow
- Create ADRs when making architectural decisions
- Evaluate quality gates before shipping

The agent doesn't need to leave the editor. Requirements, code, tests, and traceability all live in the same workflow.
