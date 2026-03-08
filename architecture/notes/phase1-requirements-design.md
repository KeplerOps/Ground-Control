# Phase 1: Requirements Management System — Design Notes

## Overview

Phase 1 introduces the first domain models in Ground Control. The requirements management system replaces the archived StrictDoc + issue-graph tools with in-app functionality, enabling Ground Control to dogfood its own requirements.

See [ADR-011](../adrs/011-requirements-data-model.md) for the formal decision record.

## App Structure

```
backend/src/ground_control/domain/requirements/
    __init__.py
    apps.py              # AppConfig (label="requirements")
    choices.py           # Enums: status, type, priority, relation_type, etc.
    models.py            # Requirement, RequirementRelation, TraceabilityLink,
                         #   GitHubIssueSync, RequirementImport
    admin.py             # Django admin registration
    parsers/
        __init__.py
        sdoc.py          # StrictDoc parser (adapted from archive/tools/issue-graph/)
    services/
        __init__.py
        requirement_service.py  # Owns: Requirement, RequirementRelation
        traceability_service.py # Owns: TraceabilityLink
        sync_service.py         # Owns: GitHubIssueSync
        import_service.py       # Owns: RequirementImport; orchestrates imports
        analysis.py             # Read-only: cycles, orphans, coverage, impact
    management/
        commands/
            import_sdoc.py        # StrictDoc import
            sync_github_issues.py # GitHub issue batch sync
            sync_age_graph.py     # AGE graph materialization
```

Infrastructure layer (shared, not requirements-specific):
```
backend/src/ground_control/infrastructure/
    github/
        client.py        # Thin wrapper around `gh api` CLI
    age/
        graph_service.py # AgeGraphService: Cypher wrapper, materialization
```

## Data Model Detail

### Requirement

The core entity. Human-readable `uid` is the external identifier (used in API paths). Internal `id` is a UUID for database operations.

```python
class Requirement(models.Model):
    id              = UUIDField(primary_key=True, default=uuid4)
    uid             = CharField(max_length=50, unique=True)      # "W2-RISK", "REQ-001"
    title           = CharField(max_length=255)
    statement       = TextField()
    rationale       = TextField(blank=True, default="")
    requirement_type = CharField(choices=RequirementType)        # functional, non_functional, constraint, interface
    priority        = CharField(choices=Priority)                # must, should, could, wont (MoSCoW)
    status          = CharField(choices=Status, default="draft") # draft, active, deprecated, archived
    wave            = IntegerField(null=True, blank=True)        # sdoc import compat
    tags            = ArrayField(TextField(), default=list)
    custom_fields   = JSONField(default=dict)
    created_at      = DateTimeField(auto_now_add=True)
    updated_at      = DateTimeField(auto_now=True)
    archived_at     = DateTimeField(null=True, blank=True)       # soft delete
```

**Status transitions** (enforced via icontract):
- `draft` -> `active`
- `active` -> `deprecated`
- `active` -> `archived` (soft delete)
- `deprecated` -> `archived`
- No transitions out of `archived`
- No backward transitions (e.g., `active` -> `draft`)

**Soft delete**: Setting `archived_at` to a timestamp. Default manager excludes archived records. A separate `all_objects` manager includes them.

### RequirementRelation

DAG edges. A requirement can have multiple parents (refines two higher-level requirements) and multiple children.

```python
class RequirementRelation(models.Model):
    id            = UUIDField(primary_key=True, default=uuid4)
    source        = ForeignKey(Requirement, related_name="outgoing_relations")
    target        = ForeignKey(Requirement, related_name="incoming_relations")
    relation_type = CharField(choices=RelationType)  # parent, depends_on, refines, conflicts, supersedes, related
    description   = TextField(blank=True, default="")
    created_at    = DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = [("source", "target", "relation_type")]
```

**Validation**: `source != target` (no self-loops). Cycle detection is an analysis-time check, not a save-time constraint (too expensive for real-time validation).

### TraceabilityLink

Connects requirements to external artifacts. The `artifact_identifier` uses a typed prefix convention:
- `github:#42` — GitHub issue
- `file:backend/src/ground_control/models.py` — code file
- `adr:011` — architecture decision record
- `test:backend/tests/unit/test_foo.py` — test file
- `config:docker-compose.yml` — configuration file

```python
class TraceabilityLink(models.Model):
    id                  = UUIDField(primary_key=True, default=uuid4)
    requirement         = ForeignKey(Requirement, related_name="traceability_links")
    artifact_type       = CharField(choices=ArtifactType)   # github_issue, code_file, adr, config, policy, test, documentation
    artifact_identifier = CharField(max_length=500)
    artifact_url        = URLField(blank=True, default="")
    artifact_title      = CharField(max_length=255, blank=True, default="")
    link_type           = CharField(choices=LinkType)       # implements, tests, documents, constrains
    sync_status         = CharField(choices=SyncStatus, default="synced")  # synced, stale, broken
    last_synced_at      = DateTimeField(null=True, blank=True)
    created_at          = DateTimeField(auto_now_add=True)
    updated_at          = DateTimeField(auto_now=True)
```

### GitHubIssueSync

Cached mirror of GitHub issue data. Updated by the `sync_github_issues` management command.

```python
class GitHubIssueSync(models.Model):
    id               = UUIDField(primary_key=True, default=uuid4)
    issue_number     = IntegerField(unique=True)
    issue_title      = CharField(max_length=500)
    issue_state      = CharField(choices=IssueState)     # open, closed
    issue_labels     = JSONField(default=list)            # ["phase-1", "requirements"]
    issue_body       = TextField(default="")
    issue_url        = URLField()
    phase            = IntegerField(null=True, blank=True)
    priority_label   = CharField(max_length=10, blank=True, default="")
    cross_references = JSONField(default=list)            # [42, 53, 101]
    last_fetched_at  = DateTimeField()
    created_at       = DateTimeField(auto_now_add=True)
    updated_at       = DateTimeField(auto_now=True)
```

### RequirementImport

Audit trail for each import/sync operation.

```python
class RequirementImport(models.Model):
    id          = UUIDField(primary_key=True, default=uuid4)
    source_type = CharField(choices=ImportSourceType)   # strictdoc, github, manual
    source_file = CharField(max_length=500, blank=True, default="")
    imported_at = DateTimeField(auto_now_add=True)
    stats       = JSONField(default=dict)               # {"created": 10, "updated": 5, "skipped": 2}
    errors      = JSONField(default=list)               # [{"uid": "REQ-001", "error": "..."}]
```

## Service Layer Architecture

### The Problem

All five models live in one Django app and share a database. Without discipline, any code can `Requirement.objects.create()` or `TraceabilityLink.objects.filter()` from anywhere, and the "clean architecture" from ADR-008 becomes fiction.

### The Solution: Write Ownership, Read Freedom

Each model has exactly **one owning service** that is the sole writer. Other services may read (via ORM querysets) but never mutate another service's models directly. Cross-service mutations go through the owning service's public interface.

```
                        API Layer (api/requirements.py)
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
     RequirementService  TraceabilityService  SyncService
       (writes)            (writes)           (writes)
     ┌──────────┐      ┌──────────────┐   ┌──────────────┐
     │Requirement│      │Traceability- │   │GitHubIssue-  │
     │Requirement│      │  Link        │   │  Sync        │
     │ Relation  │      └──────────────┘   └──────────────┘
     └──────────┘
                    AnalysisService (reads all, writes none)
                    ImportService → orchestrates other services
```

### Ownership Table

| Model | Owning Service | Allowed Readers |
|-------|---------------|-----------------|
| Requirement | RequirementService | all |
| RequirementRelation | RequirementService | AnalysisService |
| TraceabilityLink | TraceabilityService | AnalysisService, SyncService |
| GitHubIssueSync | SyncService | TraceabilityService |
| RequirementImport | ImportService | any |

### Rules

1. **One writer per model.** `SyncService` never calls `Requirement.objects.create()`. If it needs a Requirement, it calls `RequirementService.get_or_create_by_uid()`.

2. **Services expose a public interface, not querysets.** A service method returns model instances or dataclasses — callers don't chain `.filter().select_related()` across ownership boundaries.

3. **Orchestration lives in management commands or API views, not in services.** The `import_sdoc` command calls `RequirementService.create()`, then `RequirementService.create_relation()`, then `TraceabilityService.create_link()` — it's the coordinator. Services don't call each other horizontally.

4. **AnalysisService is read-only.** It queries across all models but mutates nothing. This is safe because reads don't create coupling — only writes do.

5. **No Django signals for cross-service communication.** Signals create invisible coupling. If service A needs to react to service B's writes, make it explicit: the management command or API view calls both services in sequence.

### Why Not Separate Django Apps?

Splitting into `requirements`, `traceability`, `sync` apps would give DB-level separation via different migration histories and `app_label` namespaces. But:

- **Foreign keys still cross apps** — `TraceabilityLink.requirement` is a FK to `Requirement` regardless of which app owns it. Django handles cross-app FKs fine, but it doesn't add real decoupling.
- **Migration ordering gets painful** — cross-app FKs create migration dependencies that Django resolves, but developers must reason about.
- **Premature split** — we have 5 models. Splitting into 3+ apps creates overhead without benefit. If the domain grows to 15+ models with clearly independent lifecycles, revisit.

Service-layer ownership gives us the decoupling benefits where they matter (mutation paths, testing, reasoning about side effects) without the overhead of multiple apps.

### Pragmatic Note on ADR-008

ADR-008 says the domain layer should have "zero framework imports." With Django, the models *are* the framework — `models.Model`, `UUIDField`, `ForeignKey` are all Django. This is the known tradeoff of choosing Django (ADR-010): we get batteries-included at the cost of framework coupling in the domain layer. The service layer pattern mitigates this by keeping *business logic* in services that are testable without the database (pass in model instances, mock querysets), even though the models themselves are Django-coupled.

## Key Patterns

### icontract Preconditions

Status transitions use icontract `@require` decorators on a `transition_status()` method:

```python
VALID_TRANSITIONS = {
    "draft": {"active"},
    "active": {"deprecated", "archived"},
    "deprecated": {"archived"},
    "archived": set(),
}

@require(lambda self, new_status: new_status in VALID_TRANSITIONS.get(self.status, set()))
def transition_status(self, new_status: str) -> None:
    ...
```

### Soft Delete Pattern

```python
class RequirementManager(models.Manager):
    def get_queryset(self):
        return super().get_queryset().filter(archived_at__isnull=True)

class Requirement(models.Model):
    objects = RequirementManager()          # default: excludes archived
    all_objects = models.Manager()          # includes archived
```

### AGE Graph Materialization

AGE is a read-only projection. The materialization flow:

1. Management command `sync_age_graph` runs
2. `AgeGraphService.materialize_graph()` iterates all Requirement and RequirementRelation records
3. Creates Cypher `MERGE` statements for nodes and edges
4. Executes via raw SQL through Django's `connection.cursor()`

Graph schema:
```cypher
-- Nodes
(:Requirement {uid, title, status, wave, requirement_type, priority})

-- Edges (one type per relation_type)
[:PARENT], [:DEPENDS_ON], [:REFINES], [:CONFLICTS], [:SUPERSEDES], [:RELATED]
```

### GitHub Client

Thin wrapper — no abstraction beyond what's needed:

```python
class GitHubClient:
    def __init__(self, repo: str = "KeplerOps/Ground-Control"):
        self.repo = repo

    def fetch_issues(self, state: str = "all") -> list[dict]:
        result = subprocess.run(
            ["gh", "api", f"repos/{self.repo}/issues", "--paginate", "-q", ".[]"],
            capture_output=True, text=True, check=True,
        )
        return json.loads(f"[{result.stdout}]")  # paginated output needs array wrapping
```

## References

- [ADR-011: Requirements Data Model](../adrs/011-requirements-data-model.md)
- [ADR-010: Django + django-ninja](../adrs/010-evaluate-django-framework.md)
- [ADR-008: Clean Architecture](../adrs/008-clean-architecture.md)
- Archived StrictDoc file: `archive/docs/requirements/project.sdoc`
- Archived issue-graph tool: `archive/tools/issue-graph/issue_graph.py`
