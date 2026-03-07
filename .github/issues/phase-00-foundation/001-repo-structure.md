---
title: "Establish repository structure and monorepo layout"
labels: [foundation, architecture, devex]
phase: 0
priority: P0
---

## Description

Define and create the canonical directory layout for the Ground Control monorepo. This structure must cleanly separate the Python backend, React frontend, shared schemas, deployment artifacts, documentation, and plugin SDK — while keeping everything in a single repository for atomic commits and unified CI.

## References

- PRD: Section 10 (Release Roadmap — v0.1 Foundation)
- Architecture: Section 7 (Technology Stack Summary)
- Deployment: Section 2 (Docker Compose)

## Proposed Structure

```
Ground-Control/
├── .github/
│   ├── workflows/          # CI/CD pipelines
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── backend/
│   ├── src/
│   │   └── ground_control/
│   │       ├── __init__.py
│   │       ├── main.py           # FastAPI app entry
│   │       ├── config.py         # pydantic-settings
│   │       ├── api/              # Route handlers (v1/)
│   │       ├── domain/           # Domain models & services
│   │       ├── infrastructure/   # DB, S3, cache, search adapters
│   │       ├── schemas/          # Pydantic request/response schemas
│   │       ├── middleware/       # Tenant, auth, logging, request-id
│   │       ├── events/           # Domain event bus
│   │       ├── exceptions/       # Shared exception hierarchy
│   │       ├── logging/          # Structured logging setup
│   │       └── plugins/          # Plugin runtime
│   ├── tests/
│   ├── migrations/               # Alembic
│   ├── pyproject.toml
│   └── alembic.ini
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
├── sdks/
│   ├── python/                   # Agent SDK (Python)
│   └── typescript/               # Agent SDK (TypeScript)
├── plugins/
│   ├── frameworks/               # Built-in framework definitions
│   └── integrations/             # Built-in integration plugins
├── deploy/
│   ├── docker/
│   │   ├── Dockerfile
│   │   ├── Dockerfile.frontend
│   │   └── docker-compose.yml
│   ├── helm/
│   └── terraform/
├── docs/                         # Existing design docs
├── architecture/                 # C4/Structurizr models, ADRs
│   ├── adrs/
│   ├── c4/
│   └── policies/                 # Policy-as-code (Rego/YAML)
├── .editorconfig
├── .pre-commit-config.yaml
├── CLAUDE.md
└── README.md
```

## Acceptance Criteria

- [ ] Directory structure created with placeholder `__init__.py` and `.gitkeep` files
- [ ] Root `README.md` updated to describe structure
- [ ] `CLAUDE.md` created with project conventions for AI-assisted development
- [ ] `.gitignore` covers Python (`__pycache__`, `.venv`, `.mypy_cache`), Node (`node_modules`, `dist`), IDE files, `.env`
- [ ] All existing `docs/` content remains intact and accessible

## Technical Notes

- Use a flat `src/ground_control/` layout (not nested `src/src/`) for clean imports
- Backend package name: `ground_control` (underscore, PEP 8)
- Keep `deploy/` separate from app code for clean Docker contexts
