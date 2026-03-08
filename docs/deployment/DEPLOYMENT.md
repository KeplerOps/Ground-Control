# Ground Control — Development Environment

## Prerequisites

- Python 3.12+
- Docker Engine 24+ and Docker Compose v2
- [uv](https://docs.astral.sh/uv/) (recommended) or pip

## Setup

```bash
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
cp .env.example .env
make up
make install
cd backend && . .venv/bin/activate
python manage.py migrate
python manage.py runserver 0.0.0.0:8000
```

## Docker Compose Services

The `docker-compose.yml` in the project root runs infrastructure only. Django runs on the host.

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `db` | `apache/age:release_PG16_1.6.0` | 5432 | Primary database (PostgreSQL 16 + Apache AGE 1.6.0) |
| `redis` | `redis:7` | 6379 | Cache and task queue |

PostgreSQL data persists in the `gc-postgres-data` named volume.

## Environment Variables

All settings use the `GC_` prefix. See `.env.example`.

| Variable | Default | Description |
|----------|---------|-------------|
| `GC_SECRET_KEY` | `insecure-change-me-in-production` | Django secret key |
| `GC_DEBUG` | `false` | Enable debug mode |
| `GC_DATABASE_URL` | `postgres://localhost:5432/ground_control` | PostgreSQL connection URL |
| `GC_REDIS_URL` | `redis://localhost:6379/0` | Redis connection URL |

## Makefile Targets

| Target | Command |
|--------|---------|
| `make up` | `docker compose up -d` |
| `make down` | `docker compose down` |
| `make dev` | Start Django dev server on `:8000` |
| `make install` | Create venv and install dependencies |
| `make lint` | `ruff check` + `mypy` |
| `make format` | `ruff format` |
| `make test` | `pytest` |
| `make docker-build` | Build production Docker image |

## Production Docker Image

The backend ships as a multi-stage Docker image (`backend/Dockerfile`):

- **Builder stage**: installs dependencies with `uv`, collects static files
- **Runtime stage**: minimal `python:3.12-slim`, runs as non-root user `gc` (UID 1000)
- **WSGI server**: gunicorn with 4 workers on port 8000

### Build locally

```bash
make docker-build
# or directly:
docker build -t ghcr.io/keplerops/ground-control:latest backend/
```

### Run locally

```bash
docker run --rm -p 8000:8000 \
  -e GC_SECRET_KEY=change-me \
  -e GC_DATABASE_URL=postgres://user:pass@host:5432/ground_control \
  ghcr.io/keplerops/ground-control:latest
```

Migrations are **not** baked into the image. Run them as a separate init step:

```bash
docker run --rm \
  -e GC_SECRET_KEY=change-me \
  -e GC_DATABASE_URL=postgres://user:pass@host:5432/ground_control \
  ghcr.io/keplerops/ground-control:latest \
  python manage.py migrate
```

### CI/CD

The `docker.yml` GitHub Actions workflow automatically builds and pushes to GHCR on:
- Push to `main`
- Semver tags (`v*`)

CI (lint, typecheck, tests) must pass before the image is built.

## Resetting

```bash
make down              # stop services, keep data
docker compose down -v # stop services, delete all data
```
