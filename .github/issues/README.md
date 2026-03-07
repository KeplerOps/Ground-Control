# Ground Control — Implementation Issues

This directory contains all implementation issues organized by phase. Each issue file is designed to be imported into GitHub Issues. Issues are cross-referenced to user stories, use cases, and design documents.

## Phase Summary

| Phase | Name | Issues | PRD Alignment | Priority |
|---|---|---|---|---|
| **0** | Project Bootstrap & Engineering Foundation | 001–025 | Pre-v0.1 | P0 |
| **1** | Core Data Model & Persistence | 026–040 | v0.1 | P0 |
| **2** | API Foundation & Auth | 041–055 | v0.1 | P0 |
| **3** | Authorization & Identity | 056–063 | v0.1–v0.2 | P0 |
| **4** | Core Business Logic | 064–077 | v0.1 | P0 |
| **5** | Event System & Workflow | 078–084 | v0.2 | P0–P1 |
| **6** | Frameworks & Templates | 085–092 | v0.3 | P0–P1 |
| **7** | Agent System | 093–099 | v0.4 | P0–P1 |
| **8** | Reporting & Search | 100–103 | v0.5 | P0–P1 |
| **9** | Frontend | 104–113 | v0.1–v0.5 | P0–P1 |
| **10** | Plugin System | 114–116 | v1.0 | P0–P1 |
| **11** | Production Readiness | 117–127 | v1.0 | P0–P1 |

**Total: 127 issues across 12 phases**

## Cross-Cutting Concerns

These issues establish patterns used throughout the entire codebase:

| Concern | Issue(s) | Description |
|---|---|---|
| **Coding Standards** | #003, #007, #008, #009 | Ruff, mypy, ESLint, pre-commit |
| **Architecture as Code** | #002, #024 | ADRs, C4 models, import-linter |
| **Policy as Code** | #025, #057 | Authorization policies as testable artifacts |
| **Formal Verification** | #023 | icontract, CrossHair, Hypothesis, deal |
| **Structured Logging** | #018 | structlog, JSON, context propagation |
| **Exception Handling** | #017 | Typed hierarchy, HTTP mapping, scrubbing |
| **Audit Logging** | #039 | Append-only, hash-chained, tamper-proof |
| **Configuration** | #019 | pydantic-settings, fail-fast validation |
| **Base Schemas** | #020 | API envelope, pagination, error format |
| **CI/CD Security** | #012–#016 | SonarQube, SAST, DAST, OpenANT, deps |

## Design Document References

| Document | Path | Key Issues |
|---|---|---|
| PRD | `docs/PRD.md` | All issues |
| Architecture | `docs/architecture/ARCHITECTURE.md` | #001–#025, #041, #078 |
| Data Model | `docs/architecture/DATA_MODEL.md` | #026–#040 |
| API Spec | `docs/api/API_SPEC.md` | #041–#055, #093–#099 |
| User Stories | `docs/user-stories/USER_STORIES.md` | #044–#050, #064–#077 |
| Use Cases | `docs/user-stories/USE_CASES.md` | #044–#050, #064–#077 |
| Deployment | `docs/deployment/DEPLOYMENT.md` | #006, #119–#124 |

## User Story Coverage

| Epic | User Stories | Primary Issues |
|---|---|---|
| Risk Management | US-1.1–US-1.5 | #029, #044, #064–#067, #106 |
| Control Management | US-2.1–US-2.3 | #030–#032, #045, #068–#069, #107 |
| Assessment & Testing | US-3.1–US-3.5 | #033–#034, #046, #070–#072, #108 |
| Evidence Management | US-4.1–US-4.4 | #035, #047, #073–#075, #109 |
| Findings & Remediation | US-5.1–US-5.3 | #036–#037, #048, #076, #110 |
| Reporting | US-6.1–US-6.3 | #101–#103, #112 |
| Administration | US-7.1–US-7.5 | #026–#028, #038, #051–#063, #111 |
| Agent System | US-8.1–US-8.4 | #093–#099 |

## Technology Decisions

| Decision | Choice | Issue |
|---|---|---|
| Backend Language | Python 3.12+ | #004 |
| API Framework | FastAPI | #041 |
| Database | PostgreSQL 16+ | #021 |
| ORM | SQLAlchemy 2.0 + Alembic | #021, #022 |
| Frontend | React + TypeScript + Vite | #005, #104 |
| UI Components | Shadcn/ui + Tailwind CSS | #104 |
| Search | Meilisearch | #100 |
| Cache/Queue | Redis/Valkey | #079 |
| Object Storage | S3-compatible (MinIO) | #035, #073 |
| CI/CD | GitHub Actions | #008–#016 |
| Linting | Ruff (Python), ESLint (TS) | #003, #008 |
| Type Checking | mypy (Python), tsc (TS) | #003, #009 |
| Testing | pytest, vitest, Playwright | #010, #126 |
| Code Quality | SonarQube/SonarCloud | #012 |
| SAST | Semgrep + Bandit | #013 |
| DAST | OWASP ZAP | #014 |
| AI Security | OpenANT (Knostic) | #015 |
| Formal Verification | icontract + CrossHair + deal | #023 |
| Containerization | Docker + Docker Compose | #011, #119 |
| Orchestration | Kubernetes (Helm) | #120 |
