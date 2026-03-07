# Ground Control — Open IT Risk Management Platform

Ground Control is an open, self-hostable IT Risk Management (ITRM) platform
designed to replace proprietary GRC tools like AuditBoard ITRM. It provides a
modern, API-first, plugin-extensible system for managing IT risk assessments,
control testing, evidence collection, and compliance reporting across frameworks
such as SOX ITGC, SOC 2, ISO 27001, NIST CSF/800-53, COBIT, and PCI-DSS.

## Key Principles

- **Open & Self-Hostable** — Deploy on-prem, in your cloud, or use a managed instance.
- **API-First** — Every capability is available through a versioned REST + GraphQL API.
- **Agent-Ready** — First-class support for AI agents performing assessments.
- **Plugin Architecture** — Extend with custom frameworks, integrations, and workflows.
- **Artifact-Centric** — Documents, evidence, and work products are first-class objects with full lineage.
- **Common Language** — Shared taxonomy and reusable control/risk libraries across the org.
- **Flexible SSO** — SAML 2.0, OIDC, SCIM provisioning out of the box.

## Documentation

| Document | Description |
|----------|-------------|
| [Product Requirements (PRD)](docs/PRD.md) | Full product requirements document |
| [User Stories & Use Cases](docs/user-stories/USER_STORIES.md) | Detailed user stories with acceptance criteria |
| [Use Cases (UML)](docs/user-stories/USE_CASES.md) | UML use case diagrams and descriptions |
| [Architecture](docs/architecture/ARCHITECTURE.md) | System architecture, component diagrams, data flow |
| [Data Model](docs/architecture/DATA_MODEL.md) | Entity-relationship model and storage design |
| [API Specification](docs/api/API_SPEC.md) | REST & GraphQL API design and plugin architecture |
| [Deployment & SSO](docs/deployment/DEPLOYMENT.md) | Deployment topologies, SSO configuration, operations |

## License

Apache 2.0 — see [LICENSE](LICENSE).
