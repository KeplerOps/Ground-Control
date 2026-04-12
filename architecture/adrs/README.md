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
| [001](001-django-backend.md) | Python 3.12+ with Django and django-ninja for Backend | Superseded by ADR-013 |
| [002](002-postgresql-database.md) | PostgreSQL as Primary Database | Accepted |
| [003](003-design-by-contract.md) | Design by Contract with icontract | Superseded by ADR-013 |
| [004](004-code-quality-toolchain.md) | Code Quality Toolchain | Superseded by ADR-013 |
| [005](005-apache-age-graph.md) | Apache AGE for Graph Database Capabilities | Accepted |
| [011](011-requirements-data-model.md) | Requirements Data Model | Accepted |
| [012](012-formal-methods-process.md) | Formal Methods Development Process | Accepted |
| [013](013-java-spring-boot-rewrite.md) | Java/Spring Boot Backend Rewrite | Accepted |
| [014](014-pluggable-verification-architecture.md) | Pluggable Verification Architecture | Accepted |
| [015](015-cloud-database-deployment.md) | Cloud Database Deployment | Withdrawn |
| [016](016-project-scoping.md) | Project Scoping | Accepted |
| [017](017-interactive-web-application.md) | Interactive Web Application | Accepted |
| [018](018-aws-ec2-deployment.md) | AWS EC2 Deployment | Accepted |
| [019](019-asset-topology-model.md) | Asset Topology and Boundary Relationships | Accepted |
| [020](020-asset-cross-entity-linking.md) | Asset Cross-Entity Linking | Accepted |
| [021](021-gated-agentic-development-loop.md) | Gated Agentic Development Loop | Accepted |
| [022](022-content-pack-distribution-architecture.md) | Content Pack Distribution Architecture | Accepted |
| [023](023-plugin-architecture.md) | Plugin Architecture | Accepted |
| [024](024-threat-model-entry-boundary.md) | Threat Model Entry Boundary | Accepted |

Prior ADRs from the old project frame are archived in `archive/architecture/adrs/`.
