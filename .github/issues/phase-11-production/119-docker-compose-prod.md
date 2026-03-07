---
title: "Create Docker Compose production profile"
labels: [deployment, docker, production]
phase: 11
priority: P0
---

## Description

Create a production-ready Docker Compose configuration with security hardening, health checks, and operational tooling.

## References

- Deployment: Section 2 (Docker Compose)

## Acceptance Criteria

- [ ] `deploy/docker/docker-compose.yml` (production):
  - All services with health checks and restart policies
  - Non-root user for all containers
  - Read-only filesystem where possible
  - Resource limits (memory, CPU)
  - Caddy reverse proxy with auto-TLS
  - Log drivers configured for structured JSON
- [ ] `.env.example` with all production settings documented
- [ ] `deploy/docker/Caddyfile` with security headers (HSTS, CSP, X-Frame-Options)
- [ ] MinIO auto-initialization (bucket creation)
- [ ] Backup scripts for PostgreSQL and MinIO
- [ ] Upgrade procedure documented and tested
