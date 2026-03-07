---
title: "Implement architecture as code (C4 model + architecture tests)"
labels: [foundation, architecture, quality]
phase: 0
priority: P1
---

## Description

Define the system architecture as code using C4 model diagrams (Structurizr DSL or PlantUML) and enforce architectural boundaries with import-linting tests. Architecture should be a living artifact that is verified on every CI build, not just a static document.

## References

- Architecture: All sections (system context, containers, components)
- Issue #002 (ADR Framework)

## Acceptance Criteria

- [ ] `architecture/c4/` directory with architecture models:
  - `workspace.dsl` (Structurizr DSL) or PlantUML files:
    - **System Context:** Ground Control, users, external systems
    - **Container:** API Server, Web UI, PostgreSQL, Redis, MinIO, Meilisearch, Worker
    - **Component:** Domain services (Risk, Control, Assessment, etc.)
  - Generated diagrams (PNG/SVG) committed for quick reference
- [ ] Architecture tests using `import-linter`:
  - `backend/.importlinter` configuration enforcing:
    - `api` layer can import from `domain` and `schemas`, NOT from `infrastructure`
    - `domain` layer has NO imports from `api` or `infrastructure`
    - `infrastructure` layer can import from `domain` (implements interfaces)
    - `schemas` layer has NO imports from `infrastructure`
    - No circular dependencies between domain services
  - These rules enforce Clean Architecture / Hexagonal boundaries
- [ ] CI check: `make arch-test` runs import-linter and fails on violations
- [ ] `.github/workflows/lint.yml` updated to include architecture check
- [ ] Architecture diagrams auto-generated on changes (optional GitHub Action)

## Technical Notes

- Clean Architecture layers (outside → inside):
  ```
  API (routes, middleware) → Domain (services, models, events) ← Infrastructure (DB, S3, cache)
  ```
- `import-linter` is fast and runs as part of the lint step
- Structurizr DSL can generate diagrams via Structurizr CLI or Lite — consider a GitHub Action
- The C4 model should match ARCHITECTURE.md but be the source of truth for diagrams
- Consider `pytest-archon` as an alternative to `import-linter` for more flexible rules
