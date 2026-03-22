# Ground Control — Architecture

## Mission

Ground Control is a requirements management system with traceability and graph analysis. It manages requirements, tracks relations, links to external artifacts, and runs graph-based analysis (cycles, orphans, coverage gaps, impact, cross-wave validation).

See [ADR-014](../../architecture/adrs/014-pluggable-verification-architecture.md) for the verification architecture and [ADR-011](../../architecture/adrs/011-requirements-data-model.md) for the requirements data model.

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.4 |
| Build | Gradle (Kotlin DSL) with included wrapper |
| Database | PostgreSQL 16 + Apache AGE (graph queries) |
| ORM | Hibernate 6 + Spring Data JPA |
| Auditing | Hibernate Envers |
| Migrations | Flyway |
| Contracts | JML (verified by OpenJML ESC + Z3) |
| Testing | JUnit 5 + jqwik + ArchUnit + Testcontainers |
| Static analysis | Error Prone, SpotBugs, Checkstyle |
| Formatting | Spotless + Palantir Java Format |
| Coverage | JaCoCo |
| Logging | SLF4J + Logback (JSON in prod, console in dev) |
| API docs | Springdoc-OpenAPI |
| Container | Docker (multi-stage, non-root, JDK 21) |
| Registry | GHCR (`ghcr.io/keplerops/ground-control`) |

See [ADR-013](../../architecture/adrs/013-java-spring-boot-rewrite.md) for the Java migration rationale.

## Package Structure

```
backend/src/main/java/com/keplerops/groundcontrol/
├── api/                          # REST controllers, DTOs, exception handler
│   ├── requirements/             # RequirementController, request/response records
│   ├── baselines/                # BaselineController, request/response records
│   ├── admin/                    # ImportController, SweepController, AnalysisController, GraphController, EmbeddingController
│   └── GlobalExceptionHandler.java
├── domain/                       # Business logic (Spring-web-free)
│   ├── exception/                # Domain exception hierarchy
│   ├── projects/                 # Project entity, repository, service
│   ├── baselines/                # Baseline entity, repository, service
│   └── requirements/
│       ├── model/                # JPA entities (Requirement, RequirementRelation, TraceabilityLink, RequirementEmbedding, etc.)
│       ├── repository/           # Spring Data JPA repository interfaces
│       ├── service/              # RequirementService, AnalysisService, SimilarityService, EmbeddingService, etc.
│       └── state/                # Enums (Status, RelationType, ArtifactType, LinkType, etc.)
├── infrastructure/               # External adapter implementations
│   ├── age/                      # AgeGraphService (Apache AGE Cypher queries)
│   ├── embedding/                # NoOpEmbeddingProvider, OpenAiEmbeddingProvider, config
│   ├── github/                   # GitHubCliClient (gh CLI adapter)
│   ├── sweep/                    # ScheduledSweepRunner, notifiers
│   └── web/                      # CORS config, SPA routing
├── shared/
│   └── logging/                  # RequestLoggingFilter (MDC request_id)
└── GroundControlApplication.java
```

## Dependency Rule

```
api/ -> domain/ <- infrastructure/
```

- `domain/` has no imports from `api/` or `infrastructure/` and no Spring web imports
- `api/` depends on `domain/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`

Enforced at compile time by ArchUnit tests in `ArchitectureTest.java`.

## Configuration

Spring profiles drive environment-specific behavior:

- `application.yml` — base config (datasource, JPA, Flyway, server port)
- `application-test.yml` — test overrides (Testcontainers)

Environment variables use the `GC_` prefix (e.g., `GC_DATABASE_URL`, `GC_SERVER_PORT`). See `.env.example`.

## What Exists vs. What Doesn't

### Exists (Phase 1 complete as of v0.28.0)

**Domain entities:** Requirement, RequirementRelation, TraceabilityLink, GitHubIssueSync, RequirementImport — all JPA with Envers auditing.

**Services:** RequirementService (9 methods), TraceabilityService, ImportService (StrictDoc parser + idempotent import), GitHubIssueSyncService (CLI-based GitHub sync), AnalysisService (cycle/orphan/coverage/impact/cross-wave), AgeGraphService (Apache AGE graph materialization + Cypher queries).

**API:** RequirementController (9 REST endpoints), AnalysisController (5 endpoints), ImportController, SyncController, GraphController. GlobalExceptionHandler maps domain exceptions to HTTP error envelopes.

**Tooling:** Status state machine with JML contracts (verified by OpenJML ESC + Z3), Flyway migrations (V001–V010), Spotless/Error Prone/SpotBugs/Checkstyle/JaCoCo, ArchUnit architecture tests, CI pipeline (build + test + integration + verify), production Dockerfile, GHCR publishing, E2E integration tests (6-step main + 4-step AGE).

### Does not exist yet

- Frontend
- Auth flows
- Redis integration (Redis is in docker-compose.yml but nothing in the app uses it)
- Production deployment infrastructure (local Docker Compose only)
- Multi-tenancy
- Search
- Verification result tracking (VerificationResult entity from ADR-014 not yet implemented)
- Apache AGE is optional — the app gracefully degrades to JPA-only analysis when AGE is unavailable
