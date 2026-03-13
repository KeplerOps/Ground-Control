# Ground Control — Development Environment

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker Engine 24+ and Docker Compose v2
- Gradle wrapper included (`backend/gradlew`) — no global Gradle install needed

## Setup

```bash
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
cp .env.example .env
make up                            # Start PostgreSQL + AGE
make rapid                         # Format + compile (~1s with warm daemon)
make dev                           # Spring Boot dev server on :8000
```

Flyway migrations run automatically on application startup — there is no separate migration step.

## Docker Compose Services

The `docker-compose.yml` in the project root runs infrastructure only. Spring Boot runs on the host.

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `db` | `apache/age:release_PG16_1.6.0` | 5432 | Primary database (PostgreSQL 16 + Apache AGE 1.6.0) |
| `redis` | `redis:7` | 6379 | Available for future use (not used by the application currently) |

PostgreSQL data persists in the `gc-postgres-data` named volume.

## Environment Variables

All settings use the `GC_` prefix. See `.env.example`.

| Variable | Default | Description |
|----------|---------|-------------|
| `GC_DATABASE_URL` | `jdbc:postgresql://localhost:5432/ground_control` | JDBC connection URL |
| `GC_DATABASE_USER` | `gc` | Database username |
| `GC_DATABASE_PASSWORD` | `gc` | Database password |
| `GC_REDIS_URL` | `redis://localhost:6379` | Redis connection URL (unused by app currently) |
| `GC_SERVER_PORT` | `8000` | HTTP server port |

## Makefile Targets

| Target | Description |
|--------|-------------|
| `make rapid` | Format + compile, no tests or static analysis (~1s warm) |
| `make build` | Build the project (no tests) |
| `make test` | Run unit tests (no static analysis) |
| `make test-cov` | Run tests with JaCoCo coverage report |
| `make format` | Format code with Spotless |
| `make lint` | Check formatting (Spotless) |
| `make check` | Full build + tests + static analysis + coverage (CI-equivalent) |
| `make integration` | Integration tests (Testcontainers) |
| `make verify` | Full verification: check + integration tests + OpenJML ESC |
| `make dev` | Start Spring Boot dev server on :8000 |
| `make up` | `docker compose up -d` |
| `make down` | `docker compose down` |
| `make docker-build` | Build production Docker image |
| `make smoke` | Build Docker image and verify Flyway + health endpoint |
| `make clean` | Remove build artifacts |

## Production Docker Image

The backend ships as a multi-stage Docker image (`backend/Dockerfile`):

- **Builder stage**: Eclipse Temurin JDK 21 Alpine, builds fat JAR with Gradle
- **Runtime stage**: Eclipse Temurin JRE 21 Alpine, runs as non-root user `gc`
- **Entrypoint**: `java -jar app.jar` on port 8000

### Build locally

```bash
make docker-build
# or directly:
docker build -t ghcr.io/keplerops/ground-control:latest backend/
```

### Run locally

```bash
docker run --rm -p 8000:8000 \
  -e GC_DATABASE_URL=jdbc:postgresql://host:5432/ground_control \
  -e GC_DATABASE_USER=gc \
  -e GC_DATABASE_PASSWORD=gc \
  ghcr.io/keplerops/ground-control:latest
```

Flyway migrations run automatically on startup — no separate migration step needed.

### Smoke test

```bash
make smoke
```

Builds the Docker image, starts a fresh PostgreSQL 16 container, runs the app against it, and verifies Flyway migrations apply and the health endpoint returns UP.

### CI/CD

The `docker.yml` GitHub Actions workflow automatically builds and pushes to GHCR on:
- Push to `main` or `dev`
- Semver tags (`v*`)

CI (build, test, integration, verify) must pass before the image is built.

## Resetting

```bash
make down              # stop services, keep data
docker compose down -v # stop services, delete all data
```
