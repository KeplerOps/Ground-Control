---
title: "Set up development environment (devcontainer + Docker Compose dev profile)"
labels: [foundation, devex, docker]
phase: 0
priority: P0
---

## Description

Create a reproducible development environment using Docker Compose (dev profile) for local services and an optional VS Code devcontainer. Developers should be able to run `docker compose -f docker-compose.dev.yml up` and have PostgreSQL, Redis, MinIO, and Meilisearch available locally.

## References

- Architecture: Section 5.1 (Docker Compose deployment)
- Deployment: Section 2 (Docker Compose — adapted for dev)

## Acceptance Criteria

- [ ] `deploy/docker/docker-compose.dev.yml` with:
  - PostgreSQL 16 (port 5432, with healthcheck)
  - Redis 7 (port 6379)
  - MinIO (ports 9000/9001, auto-creates `gc-artifacts` bucket)
  - Meilisearch (port 7700)
  - No app container (developers run the app natively)
- [ ] `.env.example` with all required environment variables and safe defaults
- [ ] `Makefile` target: `make services-up` / `make services-down`
- [ ] `.devcontainer/devcontainer.json` with:
  - Python 3.12 + Node 20 base image
  - Extensions: Python, Pylance, ESLint, Prettier, Docker, GitLens
  - Post-create command: install backend + frontend deps
  - Docker Compose integration for services
- [ ] `scripts/setup-dev.sh` — idempotent script that:
  - Creates `.env` from `.env.example` if not exists
  - Starts Docker services
  - Runs database migrations
  - Seeds initial taxonomy data
  - Prints "ready" message with URLs

## Technical Notes

- Dev MinIO should auto-create bucket via `mc` init container or entrypoint script
- Use Docker healthchecks so `depends_on` with `condition: service_healthy` works
- PostgreSQL should use same version (16) as production target
