# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for Ground Control. ADRs capture significant architectural decisions along with their context, rationale, and consequences.

## Format

We use [MADR](https://adr.github.io/madr/) (Markdown Any Decision Records). Each ADR includes:

- **Status**: `proposed`, `accepted`, `deprecated`, or `superseded by ADR-XXX`
- **Context**: The problem or situation driving the decision
- **Decision**: What we chose and why
- **Consequences**: Trade-offs (positive, negative, risks)

## Principles

- ADRs are **immutable** once accepted. To reverse a decision, create a new ADR that supersedes it.
- ADRs are **numbered sequentially** and never reused.
- ADRs are **versioned with code** — they live in the repo, not a wiki.

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [000](000-template.md) | ADR Template | — |
| [001](001-python-fastapi-backend.md) | Python 3.12+ with FastAPI for Backend | Accepted |
| [002](002-postgresql-primary-database.md) | PostgreSQL 16+ as Primary Database | Accepted |
| [003](003-api-first-design.md) | API-First Design (REST) | Accepted |
| [004](004-plugin-architecture.md) | Plugin Architecture for Extensibility | Accepted |
| [005](005-event-driven-architecture.md) | Event-Driven Architecture with Domain Events | Accepted |
| [006](006-multi-tenancy-strategy.md) | Multi-Tenancy Strategy (Shared Schema Default) | Accepted |
| [007](007-agent-first-design.md) | Agent-First Design (AI Agents as First-Class Actors) | Accepted |
| [008](008-clean-architecture.md) | Clean Architecture (API / Domain / Infrastructure) | Accepted |
| [009](009-coding-standards.md) | Coding Standards and Tooling | Accepted |
