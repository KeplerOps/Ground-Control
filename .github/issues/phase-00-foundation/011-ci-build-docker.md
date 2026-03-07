---
title: "CI pipeline: Build and Docker image publishing"
labels: [foundation, ci-cd, docker]
phase: 0
priority: P0
---

## Description

Create a GitHub Actions workflow that builds the application, creates Docker images, and publishes them to GitHub Container Registry (ghcr.io). This workflow should produce multi-arch images for both `linux/amd64` and `linux/arm64`.

## References

- Architecture: Section 5 (Deployment — Docker)
- Architecture: Section 7 (Containerization — Docker)
- Deployment: Section 2 (Docker Compose)

## Acceptance Criteria

- [ ] `deploy/docker/Dockerfile` (multi-stage):
  - Stage 1: Python build (install deps, compile)
  - Stage 2: Frontend build (npm install, vite build)
  - Stage 3: Production runtime (slim base, copy artifacts, non-root user)
  - Uses `python:3.12-slim` as base
  - Runs as non-root user (`gc`)
  - Health check: `CMD curl -f http://localhost:8000/health || exit 1`
- [ ] `.github/workflows/build.yml` workflow:
  - Triggers on: push to `main`, tags (`v*`), pull requests (build only, no push)
  - Build Docker image with `docker/build-push-action`
  - Push to `ghcr.io/keplerops/ground-control` on main/tags
  - Tag strategy: `latest`, git SHA, semver from tags
  - Multi-arch: `linux/amd64`, `linux/arm64`
  - SBOM generation attached to image
- [ ] Image size < 500MB
- [ ] `make docker-build` target for local builds
- [ ] Docker image labels (OCI standard): maintainer, version, source URL

## Technical Notes

- Use Docker layer caching (`actions/cache` or `type=gha` cache) for fast rebuilds
- SBOM via `docker/sbom-action` or Syft
- Consider separate Dockerfile for frontend (Nginx static serving) for K8s deployments
