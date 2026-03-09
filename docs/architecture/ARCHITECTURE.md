# Ground Control — Architecture

## Mission

Ground Control is a verification-aware software lifecycle orchestrator with graph-native artifact traceability. It manages requirements, traces artifacts across the development lifecycle, and integrates formal verification tools to ensure correctness at every stage.

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
│   └── GlobalExceptionHandler.java
├── domain/                       # Business logic (Spring-web-free)
│   ├── exception/                # Domain exception hierarchy
│   └── requirements/
│       ├── model/                # JPA entities (Requirement, RequirementRelation)
│       ├── repository/           # Spring Data JPA repository interfaces
│       ├── service/              # RequirementService, command records
│       └── state/                # Enums (Status, RelationType, Priority, RequirementType)
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

Enforced at compile time by ArchUnit tests. See [ADR-008](../../architecture/adrs/008-clean-architecture.md).

## Configuration

Spring profiles drive environment-specific behavior:

- `application.yml` — base config (datasource, JPA, Flyway, server port)
- `application-test.yml` — test overrides (Testcontainers)

Environment variables use the `GC_` prefix (e.g., `GC_DATABASE_URL`, `GC_SERVER_PORT`). See `.env.example`.

## What Exists vs. What Doesn't

**Exists:** Spring Boot application scaffold, Requirement + RequirementRelation domain model with JPA/Envers, RequirementService (9 methods), RequirementController (9 REST endpoints), Status state machine (EnumMap transitions, JML contracts on L1 methods), Flyway migrations (V001-V005), exception hierarchy with GlobalExceptionHandler, ArchUnit architecture tests, OpenJML ESC integration, Spotless/Error Prone/SpotBugs/Checkstyle/JaCoCo, CI pipeline (build + test + integration + verify), production Dockerfile, GHCR publishing.

**Does not exist yet:** Traceability links (Phase 1C), verification result tracking, graph queries via Apache AGE, auth flows, frontend, search, multi-tenancy.
