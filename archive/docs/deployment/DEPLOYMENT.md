# Ground Control — Development Environment

## Prerequisites

- Python 3.12+
- Docker Engine 24+ and Docker Compose v2
- [uv](https://docs.astral.sh/uv/) (recommended) or pip

## Setup

```bash
# Clone the repository
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control

# Copy environment template and edit as needed
cp .env.example .env

# Start PostgreSQL 16 and Redis 7
make up

# Install Python dependencies
make install

# Activate the virtualenv
cd backend && . .venv/bin/activate

# Run database migrations
python manage.py migrate

# Start the development server
python manage.py runserver 0.0.0.0:8000
# Or from the project root: make dev
```

## Docker Compose Services

The `docker-compose.yml` in the project root runs infrastructure services only. The Django app runs on the host.

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `db` | `postgres:16` | 5432 | Primary database |
| `redis` | `redis:7` | 6379 | Cache and task queue |

PostgreSQL data persists in the `gc-postgres-data` named volume.

## Environment Variables

All application settings use the `GC_` prefix. See `.env.example` for defaults.

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

## Stopping and Resetting

```bash
# Stop services (data preserved)
make down

# Stop services and delete all data
docker compose down -v
```
