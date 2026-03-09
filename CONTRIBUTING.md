# Contributing to Ground Control

## Getting Started

### Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker Engine 24+ and Docker Compose v2
- Gradle 8.x (via included wrapper — no manual install needed)

### Local Development Setup

```bash
# 1. Clone and branch
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
git checkout -b feature/your-feature dev

# 2. Start PostgreSQL and Redis
cp .env.example .env
make up

# 3. Build and test
make rapid                         # Format + compile (~1s with warm daemon)
make test                          # Unit tests

# 4. Start development server
make dev                           # Spring Boot on :8000
```

### Makefile Targets

| Target | Description |
|--------|-------------|
| `make rapid` | Format + compile, no tests or static analysis (~1s warm) |
| `make test` | Run unit tests (no static analysis) |
| `make check` | Full build + tests + static analysis + coverage (CI-equivalent) |
| `make verify` | check + integration tests + OpenJML ESC |
| `make format` | Format code with Spotless |
| `make lint` | Check formatting |
| `make integration` | Integration tests (Testcontainers) |
| `make dev` | Start Spring Boot development server |
| `make up` | Start Docker Compose services (PostgreSQL, Redis) |
| `make down` | Stop Docker Compose services |
| `make clean` | Remove build artifacts |

Use `make rapid` for the inner dev loop. Use `make check` before pushing.

## Branch Strategy

- `main` — production-ready, protected
- `dev` — integration branch, all PRs target this
- `feature/*` — feature branches, branched from `dev`

## Coding Standards

Read [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) for the complete reference. Key points below.

### Java Backend

| Tool | Purpose | Command |
|------|---------|---------|
| Spotless + Palantir | Formatting | `cd backend && ./gradlew spotlessApply` |
| Error Prone | Compile-time bug detection | Runs as part of `./gradlew check` |
| SpotBugs | Static analysis | Runs as part of `./gradlew check` |
| Checkstyle | Naming/coding patterns | Runs as part of `./gradlew check` |
| JaCoCo | Test coverage | `cd backend && ./gradlew jacocoTestReport` |

- **Records for DTOs**: Use Java records for command objects and API request/response types
- **No `var` abuse**: Use `var` only when the type is obvious from the right-hand side
- **Domain layer purity**: No Spring web imports in `domain/` (enforced by ArchUnit)

### Naming Conventions (Java)

| Element | Convention | Example |
|---------|-----------|---------|
| Packages | `lowercase` | `requirements.service` |
| Classes | `PascalCase` | `RequirementService` |
| Methods | `camelCase` | `createRelation()` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Enums | `UPPER_SNAKE_CASE` | `DEPENDS_ON` |

## Architecture Rules

The dependency rule is enforced by ArchUnit in CI:

```
api/ -> domain/ <- infrastructure/
```

- `domain/` has **zero** Spring web imports (no controllers, no HTTP)
- `api/` depends on `domain/` — never imports `infrastructure/`
- `infrastructure/` implements interfaces defined in `domain/`

See [ADR-008](architecture/adrs/008-clean-architecture.md) for rationale.

## Commit Messages

- Imperative mood: `Add risk scoring engine` not `Added risk scoring engine`
- Every commit updates `CHANGELOG.md`

## Pull Requests

- Target `dev`, not `main`
- PRs require passing CI (build + tests + static analysis + ArchUnit)
- No coverage regression
- Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md)

## Testing

```
backend/src/test/java/
├── unit/          # Domain logic only. No DB, no HTTP. JUnit 5 + Mockito.
├── property/      # jqwik property-based tests (state machines, invariants).
├── integration/   # With real PostgreSQL (Testcontainers). Spring Boot test.
└── architecture/  # ArchUnit rules (layer enforcement, naming).
```

- **JUnit 5** for unit and integration tests
- **jqwik** for property-based testing (state machines, enums)
- **Testcontainers** for integration tests (PostgreSQL 16)
- **ArchUnit** for architecture rule enforcement
- Test names describe behavior: `create_shouldThrowConflict_whenUidExists`
- Tests are independent — no shared mutable state
