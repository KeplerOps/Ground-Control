---
title: "Configure Alembic migration framework"
labels: [foundation, backend, database]
phase: 0
priority: P0
---

## Description

Set up Alembic for database schema migrations with async support, auto-generation, and a disciplined migration workflow. Every schema change must go through a migration — no manual DDL.

## References

- Data Model: Section 5 (Migration Strategy)
- Architecture: Section 7 (SQLAlchemy 2.0 + Alembic)

## Acceptance Criteria

- [ ] `backend/alembic.ini` configured:
  - Script location: `migrations`
  - SQLAlchemy URL from settings (env var)
  - File template with timestamp prefix
- [ ] `backend/migrations/` directory:
  - `env.py` — async migration runner using `asyncpg`
  - `versions/` — migration scripts
  - Target metadata imported from SQLAlchemy base
- [ ] First migration: `001_initial_schema.py` — creates empty database (just the migration infrastructure)
- [ ] Migration workflow:
  - `make migrate-create MSG="description"` — auto-generate migration
  - `make migrate-up` — apply all pending migrations
  - `make migrate-down` — rollback last migration
  - `make migrate-status` — show current state
- [ ] Migrations support:
  - Forward (`upgrade`) and backward (`downgrade`) for every migration
  - Data migrations (not just DDL)
  - Zero-downtime patterns: add column → backfill → add constraint
- [ ] CI runs migrations against a fresh database to verify they work from scratch
- [ ] Migration naming: `{timestamp}_{description}.py`

## Technical Notes

- Use `run_async` wrapper in `env.py` for async migration support
- Import all models in `env.py` so auto-generation detects them
- Add `--autogenerate` safety: review every auto-generated migration before committing
- Consider stamping migration IDs with branch names to avoid conflicts in parallel development
