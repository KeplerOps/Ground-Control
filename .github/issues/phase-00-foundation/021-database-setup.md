---
title: "Set up database connection and SQLAlchemy async engine"
labels: [foundation, backend, database]
phase: 0
priority: P0
---

## Description

Configure SQLAlchemy 2.0 async engine, session management, and base model classes for the PostgreSQL database. This forms the persistence foundation for all domain entities.

## References

- Architecture: Section 3.7 (Data & Storage — PostgreSQL 16+)
- Architecture: Section 7 (SQLAlchemy 2.0 + Alembic)
- Data Model: All entity definitions

## Acceptance Criteria

- [ ] `backend/src/ground_control/infrastructure/database/`:
  - `engine.py` — async engine creation with connection pooling (PgBouncer-compatible)
  - `session.py` — async session factory, `get_db_session` dependency for FastAPI
  - `base.py` — declarative base with common columns and mixins
- [ ] Base model class with:
  - `id: UUID` primary key (default `gen_random_uuid()`)
  - `created_at: datetime` (server default `now()`)
  - `updated_at: datetime` (auto-update on modification)
  - `__tablename__` auto-generated from class name (snake_case)
- [ ] Tenant-scoped mixin:
  - `tenant_id: UUID` foreign key to tenants
  - Index on `tenant_id` for all tenant-scoped tables
- [ ] Session middleware that sets `app.current_tenant_id` for Row-Level Security
- [ ] Connection pool settings configurable via Settings (#019):
  - `pool_size`, `max_overflow`, `pool_timeout`, `pool_recycle`
- [ ] Health check query: `SELECT 1`
- [ ] Graceful shutdown: close all connections on app shutdown
- [ ] Unit tests with async test session (use in-memory or test database)

## Technical Notes

- Use `create_async_engine` with `asyncpg` driver
- Session pattern: `async with async_session() as session:` — auto-commit/rollback
- Use `sessionmaker(class_=AsyncSession, expire_on_commit=False)` for async
- Add `repr=False` to sensitive columns to prevent accidental logging
- Design-by-contract: use `icontract.require` on session factory to validate connection params
