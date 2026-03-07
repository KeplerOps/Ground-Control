---
title: "Build documentation site (API docs, user guide, admin guide)"
labels: [documentation, production]
phase: 11
priority: P1
---

## Description

Build a comprehensive documentation site covering API reference, user guides, administrator guides, and plugin development guides.

## References

- PRD: Section 10 (v1.0 — Comprehensive documentation)
- API Spec: All sections

## Acceptance Criteria

- [ ] Documentation site (MkDocs or Docusaurus):
  - **Getting Started:** Quick start, Docker Compose, first login
  - **User Guide:** Risk management, control management, assessments, evidence, findings, reporting
  - **Admin Guide:** Installation, SSO configuration, SCIM, user management, plugins, backup/restore
  - **API Reference:** Auto-generated from OpenAPI spec (Redoc or Scalar)
  - **Agent SDK Guide:** Python and TypeScript examples, authentication, result submission
  - **Plugin Development Guide:** Plugin structure, SDK, manifest, testing, publishing
  - **Architecture Overview:** C4 diagrams, design decisions
- [ ] Hosted on GitHub Pages (auto-deployed from CI)
- [ ] Version-specific documentation (matches release versions)
- [ ] Search functionality
- [ ] `make docs` to build locally
