# Ground Control — Open IT Risk Management Platform

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KeplerOps_Ground-Control&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=KeplerOps_Ground-Control)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Ground Control is an open, self-hostable IT Risk Management (ITRM) platform
designed to replace proprietary GRC tools like AuditBoard ITRM. It provides a
modern, API-first, plugin-extensible system for managing IT risk assessments,
control testing, evidence collection, and compliance reporting across frameworks
such as SOX ITGC, SOC 2, ISO 27001, NIST CSF/800-53, COBIT, and PCI-DSS.

## Key Principles

- **Open & Self-Hostable** — Deploy on-prem, in your cloud, or use a managed instance.
- **API-First** — Every capability is available through a versioned REST API.
- **Agent-Ready** — First-class support for AI agents performing assessments.
- **Plugin Architecture** — Extend with custom frameworks, integrations, and workflows.
- **Artifact-Centric** — Documents, evidence, and work products are first-class objects with full lineage.
- **Common Language** — Shared taxonomy and reusable control/risk libraries across the org.
- **Flexible SSO** — SAML 2.0, OIDC, SCIM provisioning out of the box.

## Repository Structure

```
Ground-Control/
├── backend/                  # Python backend (Django)
│   ├── src/ground_control/   # Application source code
│   │   ├── api/              # Route handlers (v1/)
│   │   ├── domain/           # Domain models & services
│   │   ├── infrastructure/   # DB, S3, cache, search adapters
│   │   ├── schemas/          # Pydantic request/response schemas
│   │   ├── middleware/       # Tenant, auth, logging, request-id
│   │   ├── events/           # Domain event bus
│   │   ├── exceptions/       # Shared exception hierarchy
│   │   ├── logging/          # Structured logging setup
│   │   └── plugins/          # Plugin runtime
│   ├── tests/                # Unit, integration, and e2e tests
│   └── migrations/           # Django database migrations
├── frontend/                 # React + TypeScript + Vite
│   ├── src/
│   └── public/
├── sdks/                     # Agent SDKs
│   ├── python/
│   └── typescript/
├── plugins/                  # Built-in plugins
│   ├── frameworks/           # Framework definitions
│   └── integrations/         # Integration plugins
├── deploy/                   # Deployment artifacts
│   ├── docker/
│   ├── helm/
│   └── terraform/
├── architecture/             # Architecture artifacts
│   ├── adrs/                 # Architecture Decision Records
│   ├── c4/                   # C4/Structurizr models
│   └── policies/             # Policy-as-code (Rego/YAML)
└── docs/                     # Design documentation
```

## Documentation

| Document | Description |
|----------|-------------|
| [Product Requirements (PRD)](docs/PRD.md) | Full product requirements document |
| [User Stories & Use Cases](docs/user-stories/USER_STORIES.md) | Detailed user stories with acceptance criteria |
| [Use Cases (UML)](docs/user-stories/USE_CASES.md) | UML use case diagrams and descriptions |
| [Architecture](docs/architecture/ARCHITECTURE.md) | System architecture, component diagrams, data flow |
| [Data Model](docs/architecture/DATA_MODEL.md) | Entity-relationship model and storage design |
| [API Specification](docs/api/API_SPEC.md) | REST API design and plugin architecture |
| [Deployment & SSO](docs/deployment/DEPLOYMENT.md) | Deployment topologies, SSO configuration, operations |
| [Coding Standards](docs/CODING_STANDARDS.md) | Code style, architecture rules, testing conventions |

## License

MIT — see [LICENSE](LICENSE).
