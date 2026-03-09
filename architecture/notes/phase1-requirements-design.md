# Phase 1: Requirements Management System -- Design Notes

## Overview

Phase 1 introduces the first domain models in Ground Control. The requirements management system replaces the archived StrictDoc + issue-graph tools with in-app functionality, enabling Ground Control to dogfood its own requirements.

See [ADR-011](../adrs/011-requirements-data-model.md) for the formal decision record.

## Package Structure

```
backend/src/main/java/com/keplerops/groundcontrol/
    domain/
        requirements/
            model/
                Requirement.java           # @Entity + JML contracts + @Audited
                RequirementRelation.java   # @Entity + JML contracts + @Audited
            state/
                Status.java                # Enum + EnumMap transition table
                RequirementType.java       # FUNCTIONAL, NON_FUNCTIONAL, CONSTRAINT, INTERFACE
                Priority.java             # MUST, SHOULD, COULD, WONT
                RelationType.java         # PARENT, DEPENDS_ON, REFINES, CONFLICTS, SUPERSEDES, RELATED
            service/
                RequirementService.java    # Write-owner of Requirement + RequirementRelation
                CreateRequirementCommand.java  # Immutable command record
                UpdateRequirementCommand.java  # Immutable command record
            repository/
                RequirementRepository.java           # Spring Data JPA
                RequirementRelationRepository.java   # Spring Data JPA
        exception/                         # Shared across all domain areas
            GroundControlException.java
            NotFoundException.java
            DomainValidationException.java
            AuthenticationException.java
            AuthorizationException.java
            ConflictException.java
    api/
        requirements/
            RequirementController.java     # @RestController (planned)
            RequirementRequest.java        # Request DTO record (planned)
            RequirementResponse.java       # Response DTO record (planned)
            StatusTransitionRequest.java   # (planned)
            RelationRequest.java           # (planned)
            RelationResponse.java          # (planned)
        GlobalExceptionHandler.java        # @RestControllerAdvice
        ErrorResponse.java                 # {"error": {"code", "message", "detail"}}
    infrastructure/
        age/
            AgeGraphService.java           # AGE Cypher via JdbcTemplate (planned)
    shared/
        logging/
            RequestLoggingFilter.java      # MDC: request_id
```

Future domain areas (verification, risks, controls) follow the same structure under `domain/`.

## Data Model Detail

### Requirement

The core entity. Human-readable `uid` is the external identifier (used in API paths). Internal `id` is a UUID for database operations.

```java
@Entity
@Audited
@Table(name = "requirement")
public class Requirement {

    // @ public invariant archivedAt == null || status == Status.ARCHIVED;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String uid;         // "W2-RISK", "REQ-001"

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(columnDefinition = "TEXT")
    private String rationale = "";

    @Enumerated(EnumType.STRING)
    private RequirementType requirementType = RequirementType.FUNCTIONAL;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MUST;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private Integer wave;
    private Instant createdAt;   // set on @PrePersist
    private Instant updatedAt;   // set on @PreUpdate
    private Instant archivedAt;  // soft delete
}
```

**Status transitions** (enforced via JML contracts on `transitionStatus()`):
- DRAFT -> ACTIVE
- ACTIVE -> DEPRECATED
- ACTIVE -> ARCHIVED (soft delete)
- DEPRECATED -> ARCHIVED
- No transitions out of ARCHIVED
- No backward transitions

**Soft delete**: Setting `archivedAt` to a timestamp. Default queries should filter on `archivedAt IS NULL`.

### RequirementRelation

DAG edges. A requirement can have multiple parents (refines two higher-level requirements) and multiple children.

```java
@Entity
@Audited
@Table(name = "requirement_relation",
       uniqueConstraints = @UniqueConstraint(
           columns = {"source_id", "target_id", "relation_type"}))
public class RequirementRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_id")
    private Requirement source;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_id")
    private Requirement target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType relationType;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    private Instant createdAt;   // set on @PrePersist
}
```

**Validation**: `source != target` (no self-loops), enforced in `RequirementService.createRelation()`. Cycle detection is an analysis-time check, not a save-time constraint (too expensive for real-time validation).

### TraceabilityLink (planned)

Connects requirements to external artifacts. The `artifactIdentifier` uses a typed prefix convention:
- `github:#42` -- GitHub issue
- `file:backend/src/.../Requirement.java` -- code file
- `adr:011` -- architecture decision record
- `test:backend/src/test/.../RequirementTest.java` -- test file
- `tla:specs/tla/RequirementStateMachine.tla` -- TLA+ specification
- `proof:verification/results/access-control.json` -- verification result

See [ADR-011](../adrs/011-requirements-data-model.md#traceabilitylink-planned) for the full field list.

### GitHubIssueSync (planned)

Cached mirror of GitHub issue data. Updated by the sync service. See [ADR-011](../adrs/011-requirements-data-model.md#githubissuesync-planned) for the full field list.

### RequirementImport (planned)

Audit trail for each import/sync operation. See [ADR-011](../adrs/011-requirements-data-model.md#requirementimport-planned) for the full field list.

## Service Layer Architecture

### The Problem

All entities share a database and Spring context. Without discipline, any component can inject any repository and mutate any entity, and the "clean architecture" becomes fiction.

### The Solution: Write Ownership, Read Freedom

Each entity has exactly **one owning service** that is the sole writer. Other services may read (via repository queries) but never mutate another service's entities. Cross-service mutations go through the owning service's public interface.

```
                    API Layer (RequirementController)
                              |
              +---------------+---------------+
              v               v               v
     RequirementService  TraceabilityService  SyncService
       (writes)            (writes)           (writes)
     +------------+    +--------------+   +--------------+
     |Requirement |    |Traceability- |   |GitHubIssue-  |
     |Requirement |    |  Link        |   |  Sync        |
     | Relation   |    +--------------+   +--------------+
     +------------+
                    AnalysisService (reads all, writes none)
                    ImportService -> orchestrates other services
```

### Ownership Table

| Entity | Owning Service | Allowed Readers |
|--------|---------------|-----------------|
| Requirement | RequirementService | all |
| RequirementRelation | RequirementService | AnalysisService |
| TraceabilityLink | TraceabilityService | AnalysisService, SyncService |
| GitHubIssueSync | SyncService | TraceabilityService |
| RequirementImport | ImportService | any |

### Rules

1. **One writer per entity.** `SyncService` never calls `requirementRepository.save()`. If it needs a Requirement, it calls `RequirementService.getByUid()`.

2. **Services expose a public interface, not repositories.** A service method returns entities or records -- callers don't chain repository queries across ownership boundaries.

3. **Orchestration lives in controllers or application services, not in domain services.** The import flow calls `RequirementService.create()`, then `RequirementService.createRelation()`, then `TraceabilityService.createLink()` -- it's the coordinator. Domain services don't call each other horizontally.

4. **AnalysisService is read-only.** It queries across all repositories but mutates nothing. This is safe because reads don't create coupling -- only writes do.

5. **No Spring events for cross-service communication.** Events create invisible coupling. If service A needs to react to service B's writes, make it explicit: the controller or orchestration service calls both services in sequence.

### Transactional Boundaries

- `@Transactional` on the service class (default for all methods)
- `@Transactional(readOnly = true)` on read-only methods (enables Hibernate query optimizations)
- Controllers are not transactional -- the service boundary is the transaction boundary
- Orchestration across services uses the default propagation (`REQUIRED`) so all calls within one controller action share a transaction

### Why Not Separate Packages?

Splitting into separate top-level packages (`requirements`, `traceability`, `sync`) would give clearer boundaries. But:

- **Foreign keys still cross packages** -- `TraceabilityLink.requirement` is a FK to `Requirement` regardless of which package owns it.
- **Migration ordering gets painful** -- cross-package FKs create Flyway ordering dependencies.
- **Premature split** -- we have 5 entities. Splitting into 3+ packages creates overhead without benefit. If the domain grows to 15+ entities with clearly independent lifecycles, revisit.

Service-layer ownership gives us the decoupling benefits where they matter (mutation paths, testing, reasoning about side effects) without the overhead of premature splitting.

## Key Patterns

### JML Contracts

Status transitions use JML `requires`/`ensures` annotations:

```java
// @ requires newStatus != null;
// @ requires status.canTransitionTo(newStatus);
// @ ensures status == newStatus;
public void transitionStatus(/*@ non_null @*/ Status newStatus) {
    if (!status.canTransitionTo(newStatus)) {
        throw new DomainValidationException(
            "Cannot transition from " + status + " to " + newStatus,
            "invalid_status_transition",
            Map.of("current_status", status.name(),
                   "target_status", newStatus.name()));
    }
    this.status = newStatus;
}
```

Every JML contract has a corresponding happy-path test and violation test. See [CODING_STANDARDS.md](../../docs/CODING_STANDARDS.md) for the full SDD workflow.

### EnumMap State Machine

```java
public enum Status {
    DRAFT, ACTIVE, DEPRECATED, ARCHIVED;

    private static final EnumMap<Status, Set<Status>> VALID_TRANSITIONS =
        new EnumMap<>(Map.of(
            DRAFT,      Set.of(ACTIVE),
            ACTIVE,     Set.of(DEPRECATED, ARCHIVED),
            DEPRECATED, Set.of(ARCHIVED),
            ARCHIVED,   Set.of()));

    public Set<Status> validTargets() {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of());
    }

    public boolean canTransitionTo(Status target) {
        return validTargets().contains(target);
    }
}
```

Verified by jqwik property tests (every enum value has a transition entry, terminal states have no outgoing transitions, valid transitions change status, invalid transitions are rejected) and structural tests.

### Hibernate Envers Auditing

All domain entities are annotated with `@Audited`. Envers automatically maintains:
- `revinfo` table (revision ID + timestamp, using `revinfo_seq` sequence)
- `requirement_audit` table (all columns + `rev` FK + `revtype`)
- `requirement_relation_audit` table (same pattern, no referential integrity to requirement)

Flyway migrations V003-V005 create these tables. Audit tables use Envers default column names (`rev`, `revtype`).

### Command Records

Service mutations use immutable command records:

```java
public record CreateRequirementCommand(
    String uid,
    String title,
    String statement,
    String rationale,
    RequirementType requirementType,
    Priority priority,
    Integer wave
) {}
```

This separates the API representation (request DTOs with `@NotBlank` validation) from the domain mutation interface (commands). Controllers map: request -> command -> service call -> entity -> response.

### AGE Graph Materialization (planned)

AGE is a read-only projection. The materialization flow:

1. `AgeGraphService.materializeGraph()` iterates all Requirement and RequirementRelation records via JPA
2. Creates Cypher `MERGE` statements for nodes and edges
3. Executes via `JdbcTemplate` raw SQL (AGE has no JDBC driver -- raw SQL is the access pattern)

Graph schema:
```cypher
-- Nodes
(:Requirement {uid, title, status, wave, requirement_type, priority})

-- Edges (one type per relation_type)
[:PARENT], [:DEPENDS_ON], [:REFINES], [:CONFLICTS], [:SUPERSEDES], [:RELATED]
```

Future: VerificationResult nodes and TraceabilityLink edges will also be materialized into the graph, enabling cross-cutting queries like "show all requirements where verification has not achieved L2" (see [ADR-014](../adrs/014-pluggable-verification-architecture.md)).

## References

- [ADR-011: Requirements Data Model](../adrs/011-requirements-data-model.md)
- [ADR-013: Java/Spring Boot Backend Rewrite](../adrs/013-java-spring-boot-rewrite.md)
- [ADR-005: Apache AGE](../adrs/005-apache-age-graph.md)
- [ADR-014: Pluggable Verification Architecture](../adrs/014-pluggable-verification-architecture.md)
- Archived StrictDoc file: `archive/docs/requirements/project.sdoc`
- Archived issue-graph tool: `archive/tools/issue-graph/issue_graph.py`
