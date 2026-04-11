# Ground Control — Architecture

## Mission

Ground Control is a requirements management system with traceability and graph analysis. It manages requirements, tracks relations, links to external artifacts, and runs graph-based analysis (cycles, orphans, coverage gaps, impact, cross-wave validation).

See [ADR-014](../../architecture/adrs/014-pluggable-verification-architecture.md) for the verification architecture and [ADR-011](../../architecture/adrs/011-requirements-data-model.md) for the requirements data model.

## Stack

### Backend

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

### Frontend

| Component | Technology |
|-----------|-----------|
| Framework | React 19 |
| Language | TypeScript 5 |
| Bundler | Vite 6 |
| Routing | React Router 7 |
| Server state | TanStack Query 5 |
| Styling | Tailwind CSS 4 |
| Components | shadcn/ui (Radix primitives) |
| Graph viz | Cytoscape.js + dagre |
| Linting/Format | Biome |
| Testing | Vitest |
| Deployment | Embedded in Spring Boot static resources |

See [ADR-017](../../architecture/adrs/017-interactive-web-application.md) for the frontend decision rationale.

## Package Structure

```
backend/src/main/java/com/keplerops/groundcontrol/
├── api/                          # REST controllers, DTOs, exception handler
│   ├── requirements/             # RequirementController, request/response records
│   ├── baselines/                # BaselineController, request/response records
│   ├── admin/                    # ImportController, SweepController, AnalysisController, GraphController, EmbeddingController
│   ├── verification/             # VerificationResultController, request/response records
│   ├── plugins/                  # PluginController, request/response records
│   └── GlobalExceptionHandler.java
├── domain/                       # Business logic (Spring-web-free)
│   ├── exception/                # Domain exception hierarchy
│   ├── projects/                 # Project entity, repository, service
│   ├── baselines/                # Baseline entity, repository, service
│   ├── verification/             # VerificationResult entity, VerificationStatus/AssuranceLevel enums, repository, service
│   ├── plugins/                  # Plugin interface, PluginRegistry, RegisteredPlugin entity, PluginType/PluginLifecycleState enums
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

### Exists

**Domain entities:** Requirement, RequirementRelation, TraceabilityLink, GitHubIssueSync, RequirementImport — all JPA with Envers auditing.

**Services:** RequirementService (9 methods), TraceabilityService (forward and reverse artifact lookup), ImportService (StrictDoc parser + idempotent import), GitHubIssueSyncService (CLI-based GitHub sync), AnalysisService (cycle/orphan/coverage/impact/cross-wave), AgeGraphService (Apache AGE graph materialization + Cypher queries).

**API:** RequirementController (9 REST endpoints), AnalysisController (5 endpoints), ImportController, SyncController, GraphController. GlobalExceptionHandler maps domain exceptions to HTTP error envelopes.

**Frontend:** React 19 / TypeScript SPA served as embedded static resources from the Spring Boot JAR. Views: Dashboard (project health metrics), Requirements Explorer (browse/filter/author), Requirement Detail (fields, relations, traceability, audit), Dependency Graph (Cytoscape.js DAG visualization). See [ADR-017](../../architecture/adrs/017-interactive-web-application.md).

**Tooling:** Status state machine with JML contracts (verified by OpenJML ESC + Z3), Flyway migrations, Spotless/Error Prone/SpotBugs/Checkstyle/JaCoCo, ArchUnit architecture tests, CI pipeline (build + test + integration + verify), production Dockerfile, GHCR publishing, E2E integration tests.

### Does not exist yet

- Auth flows
- Redis integration (Redis is in docker-compose.yml but nothing in the app uses it)
- Production deployment infrastructure (local Docker Compose + EC2 via CDK)
- Multi-tenancy
- Search
- Concrete verifier adapter implementations in `infrastructure/verifiers/` (ADR-014 §6). The `VerifierAdapter` port interface and request/outcome contracts are defined in the domain layer; future work is implementing adapters for each prover (OpenJML, TLA+/TLC, OPA/Rego, Frama-C, manual review).
- Traceability Matrix view (`/traceability`) and Audit Timeline view (`/audit`) in the frontend
- Apache AGE is optional — the app gracefully degrades to JPA-only analysis when AGE is unavailable

### Exists now

- `specs/tla/` for design-level verification artifacts and state-machine specs, aligned with ADR-014
- Verification result storage (VerificationResult entity with eager-loaded target/requirement, enums, CRUD API, MCP tools) — ADR-014 §2 common schema
- Pluggable verifier adapter interface (`VerifierAdapter`, `VerificationRequest`, `VerificationOutcome`) — ADR-014 §6 port contract for multi-tool integration
- Self-referential traceability enforcement — `check_live_policy.mjs` verifies substantive code files have reverse traceability links to requirements (GC-O002), using the `GET /requirements/traceability/by-artifact` reverse lookup endpoint
