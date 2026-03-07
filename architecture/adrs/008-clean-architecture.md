# ADR-008: Clean Architecture (API / Domain / Infrastructure Layers)

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control is a complex domain application with multiple integration points (databases, object storage, identity providers, external APIs). Without clear architectural boundaries, the codebase will become a tangle of framework dependencies, making it hard to test, refactor, and evolve.

We evaluated: traditional MVC, hexagonal architecture, clean architecture, and vertical slice architecture.

## Decision

Adopt Clean Architecture with three primary layers and strict dependency rules.

```
api/ -> domain/ <- infrastructure/
```

- **Domain layer** (`domain/`): Entities, value objects, service interfaces, use cases. Zero framework imports. Independently testable.
- **API layer** (`api/`): Route handlers. Thin — parses requests, calls use cases, formats responses. Depends on `domain/` and `schemas/`.
- **Infrastructure layer** (`infrastructure/`): Database repositories, S3 clients, Redis, external API adapters. Implements interfaces defined in `domain/`.
- **Cross-cutting** (`schemas/`, `exceptions/`, `logging/`, `events/`): Importable by any layer.

Dependency rule: dependencies point inward. `domain/` never imports from `api/` or `infrastructure/`.

Enforced by `import-linter` in CI — violations fail the build.

## Consequences

### Positive

- Domain logic is testable without databases, HTTP, or any framework
- Swapping infrastructure (e.g., PostgreSQL to another DB) doesn't touch domain code
- Clear boundaries make the codebase navigable for new developers
- `import-linter` makes the architecture self-enforcing

### Negative

- More boilerplate: interfaces in domain, implementations in infrastructure
- Indirection via dependency injection adds complexity vs. direct imports
- Developers must understand the layering rules (mitigated: CI enforcement + coding standards doc)

### Risks

- Over-abstraction: risk of creating interfaces for things that will never have multiple implementations (mitigated: only abstract at real boundaries)
