# Issue Rationalization Plan

Generated from `tools/issue-graph` analysis of 124 open issues across 12 phases.

## Problem Statement

The current issue set was written for a FastAPI/SQLAlchemy stack and a SaaS deployment model. The project has since switched to Django (ADR-010) and the first deployment target is **on-prem single-tenant**. 36 issues reference removed technology. Phases are misordered — deployment (#133) is in phase-11 but should be one of the first things working, while multi-tenancy (#131) is phase-11 but blocks phase-0 issue #150.

## Guiding Principles

1. **Guardrails before code.** Formal methods, design-by-contract, CI scanning, and architecture constraints exist to railroad coding agents into producing correct code. Without independent feedback loops, agents go squirrely. Every quality gate that catches a problem before it ships is worth the setup cost.
2. **Design the contracts, then write code that satisfies them.** icontract preconditions, Rocq proof targets, architecture tests, and policy constraints define what "correct" means. Domain code is written *against* these specifications, not verified after the fact.
3. **On-prem single-tenant first.** No multi-tenancy, no Kubernetes, no schema-per-tenant until there's a second customer.
4. **Django-native first.** Use Django auth, admin, ORM, signals, middleware before building custom anything.
5. **Frontend and backend in parallel once API contracts are defined.** Don't gate frontend on phase-9.

---

## Proposed Phase Structure

### Wave 0: Foundation + CI + Quality Gates
*Everything needed before a single line of domain code gets written. Dev environment, full CI pipeline with all scanning, formal methods infrastructure, design-by-contract, architecture constraints.*

| # | Title | Notes |
|---|-------|-------|
| 14 | ADR framework | Rewrite: remove FastAPI references from body |
| 19 | Dev environment (devcontainer + Docker Compose dev) | Keep |
| 20 | Pre-commit hooks and editor settings | Keep |
| 21 | CI: Linting and formatting | Keep |
| 22 | CI: Type checking | Rewrite: remove SQLAlchemy mypy plugin ref |
| 23 | CI: Test runner | Rewrite: remove httpx ref |
| 24 | CI: Build and Docker image | Keep |
| 25 | CI: SonarQube | Keep |
| 26 | CI: SAST scanning (Semgrep + Bandit) | Keep |
| 27 | CI: DAST scanning (OWASP ZAP) | Keep — quality gates before code |
| 28 | CI: OpenANT AI vulnerability scanning | Keep — independent AI feedback loop |
| 29 | CI: Dependency and license scanning | Keep — catches supply chain issues early |
| 32 | Config management (pydantic-settings) | Rewrite: remove FastAPI ref |
| 36 | Rocq formal proof infrastructure | Define proof targets before domain code exists |
| 37 | Design-by-contract (icontract/crosshair) | Contracts define correctness before implementation |
| 38 | Architecture as code (C4 + architecture tests) | Structural constraints before code |
| 18 | MCP servers (Rocq, AWS) | Tooling for formal verification workflow |
| 133 | Docker Compose production profile | **Move from phase-11.** On-prem needs this early. |
| 135 | Health endpoints + Prometheus metrics | **Move from phase-11.** Observable from day one. |

### Wave 1: Core Django Setup
*Django auth, permissions, base schemas, logging — the cross-cutting concerns that all domain code depends on. Written against the contracts defined in Wave 0.*

| # | Title | Notes |
|---|-------|-------|
| 30 | Shared exception hierarchy | Rewrite: remove FastAPI/Starlette refs |
| 31 | Structured logging (structlog) | Keep |
| 33 | Base Pydantic schemas / response envelope | Already rewritten for django-ninja |
| 39 | Django permissions and groups | Already rewritten |
| 41 | User model | Rewrite: Django User model, not SQLAlchemy |
| 42 | Role and permission model | Rewrite: Django groups + permissions |
| 53 | Audit log (append-only) | Rewrite: use django-auditlog, not SQLAlchemy |
| 65 | Local authentication | Rewrite: Django auth backend, not Argon2id/passlib |
| 57 | API versioning and router structure | Rewrite: django-ninja routers |

**Close:**
- #40 (Tenant model + RLS) → **Close.** On-prem single-tenant. Add `tenant_id` column later.
- #56 (Tenant context middleware) → **Close.** No tenants.
- #66 (JWT token management) → **Close.** Django session auth + API keys sufficient for v0.1.
- #131 (Multi-tenancy RLS) → **Close.** Same as #40.
- #132 (Schema-per-tenant) → **Close.** Same.

### Wave 2: Domain Models
*The actual ITRM entities — Django models, migrations, admin registration. Contracts and architecture tests from Wave 0 validate these as they're built.*

| # | Title | Notes |
|---|-------|-------|
| 43 | Risk entity | Rewrite: Django model, not SQLAlchemy |
| 44 | Control entity | Rewrite: Django model |
| 45 | Framework and requirements entities | Rewrite: Django model |
| 46 | CCL entity | Rewrite: Django model |
| 47 | Assessment campaign entity | Rewrite: Django model |
| 48 | Test procedure and test steps | Rewrite: Django model |
| 49 | Artifact/evidence entity | Rewrite: Django model + django-storages |
| 50 | Finding entity | Rewrite: Django model |
| 51 | Remediation plan entity | Rewrite: Django model |
| 52 | Taxonomy configuration | Rewrite: Django model |
| 54 | Comment and notification entities | Rewrite: Django model |

All 11 phase-1 entity issues need body text rewritten to reference Django ORM, `makemigrations`, and Django admin instead of SQLAlchemy + Alembic.

### Wave 3: API Endpoints
*django-ninja endpoints for all domain models. Auth middleware.*

| # | Title | Notes |
|---|-------|-------|
| 58 | Risk API endpoints | Rewrite deps |
| 59 | Control API endpoints | Rewrite deps |
| 60 | Assessment and test procedure API | Keep |
| 61 | Artifact/evidence API | Keep |
| 62 | Finding and remediation API | Keep |
| 63 | Taxonomy, audit log, search API | Keep |
| 64 | Evidence request API | Keep |
| 67 | OIDC authentication | Keep |
| 68 | API key authentication | Keep — simpler than OAuth2 for agents |
| 69 | OpenAPI validation and docs | Rewrite: django-ninja auto-generates OpenAPI |
| 72 | Authorization middleware | Rewrite: Django middleware, not FastAPI |
| 77 | User and role management API | Keep |

**Close:**
- #70 (RBAC engine) → **Close.** Django permissions IS the RBAC engine.
- #71 (ABAC policy engine) → **Close.** Premature. Django permissions first.
- #74 (OAuth2 for agents) → **Defer to post-v0.1.** API keys (#68) sufficient.

### Wave 4: Business Logic
*Scoring, workflows, state machines, evidence management — the stuff that makes it an ITRM tool.*

| # | Title | Notes |
|---|-------|-------|
| 78 | Risk scoring engine | Keep |
| 79 | Risk assessment campaign workflow | Keep |
| 80 | Risk treatment plan management | Keep |
| 81 | Risk-control linkage | Rewrite: remove Alembic ref |
| 82 | Control catalog management | Keep |
| 83 | Control-framework mapping + gap analysis | Keep |
| 84 | Assessment campaign state machine | Keep |
| 85 | Test procedure execution engine | Keep |
| 87 | Evidence upload + S3 storage | Rewrite: remove Alembic dep, use django-storages |
| 88 | Evidence linking and requests | Keep |
| 90 | Finding lifecycle | Keep |
| 93 | Background job processing | Keep — django-q2 |

**Close or defer:**
- #86 (Sampling methodology) → Defer. Nice-to-have.
- #89 (Evidence lineage/chain of custody) → Defer. v0.2.
- #91 (Bulk import/export) → Defer. v0.2.
- #92 (Domain event bus) → **Close.** Use Django signals. Not a separate system.
- #94 (Workflow engine) → Defer. State machines in #84 are sufficient for v0.1.
- #98 (Task assignment + SLA) → Defer. v0.2.

### Wave 5: Framework Definitions + Seed Data
*The compliance content that makes the tool useful.*

| # | Title | Notes |
|---|-------|-------|
| 99 | Framework plugin loader | Keep but simplify — YAML/JSON loader, not plugin runtime |
| 100 | SOX ITGC framework | Keep |
| 101 | SOC 2 framework | Keep |
| 105 | CCL seed data | Keep |
| 106 | Template library | Keep |

**Defer:**
- #102 (ISO 27001) → v0.2. SOX + SOC 2 first.
- #103 (NIST CSF + 800-53) → v0.2.
- #104 (PCI-DSS, CIS) → v0.2.

### Wave 6: Frontend
*Can start in parallel with Wave 3 once API contracts are defined.*

| # | Title | Notes |
|---|-------|-------|
| 17 | Scaffold frontend (React + TypeScript + Vite) | Keep |
| 118 | UI component library + app shell | Keep |
| 119 | Auth pages | Keep |
| 120 | Risk management views | Keep |
| 121 | Control management views | Keep |
| 122 | Assessment/testing views | Keep |
| 123 | Evidence management views | Keep |
| 124 | Findings/remediation views | Keep |
| 125 | Admin pages | Keep |
| 126 | Dashboard/reporting views | Keep |

**Defer:**
- #127 (WCAG accessibility audit) → v0.2.

### Wave 7: Notifications + Integrations
*Only after core CRUD works end-to-end.*

| # | Title | Notes |
|---|-------|-------|
| 95 | Notification system (in-app + email) | Keep |
| 96 | Outbound webhooks | Keep |
| 115 | Dashboard data aggregation APIs | Keep |
| 116 | Report generation (PDF/PPTX/Excel) | Keep |

**Defer:**
- #97 (Slack/Teams integration) → v0.2.
- #117 (Scheduled reports + GraphQL) → v0.2. GraphQL is scope creep.
- #114 (Meilisearch) → v0.2. Django ORM queries + `__search` sufficient for v0.1.

### Wave 8: Agents
*Only after the platform works for humans.*

| # | Title | Notes |
|---|-------|-------|
| 107 | Agent registration + lifecycle | Keep |
| 108 | Agent assignment + result submission | Keep |
| 109 | Agent provenance tracking | Keep |
| 110 | Agent SDK — Python | Rewrite: remove httpx ref |
| 112 | Automated evidence collection interface | Keep |

**Defer:**
- #111 (Agent SDK — TypeScript) → v0.2. Python first.
- #113 (AI control mapping suggestions) → v0.2.

### Wave 9: Hardening + Operations
*Before going to production at client sites.*

| # | Title | Notes |
|---|-------|-------|
| 137 | Security hardening + pentest | Keep |
| 138 | Backup and disaster recovery | Keep |
| 136 | Performance benchmarking | Keep |
| 140 | E2E test suite (Playwright) | Keep |
| 141 | Documentation site | Keep |

**Defer:**
- #134 (Kubernetes Helm chart) → Post-v0.1. Docker Compose is the deployment target.
- #139 (AuditBoard import) → Only when a customer needs it.

### Deferred Entirely (post-v0.1)
| # | Title | Reason |
|---|-------|--------|
| 73 | SAML 2.0 SSO | Enterprise SSO is v0.2 |
| 75 | SCIM 2.0 provisioning | Enterprise provisioning is v0.2 |
| 76 | MFA (TOTP + WebAuthn) | v0.2 |
| 128 | Plugin runtime + sandboxing | v0.2. Framework loader (#99) is enough for v0.1 |
| 129 | Plugin SDK | v0.2 |
| 130 | Plugin management API + UI | v0.2 |

---

## Issues to Rewrite (36 stale)

All phase-1 entity issues (#40-#54) reference SQLAlchemy and Alembic. These need body text updated to reference Django ORM and `makemigrations`. The fix is mechanical — replace:
- "SQLAlchemy model" → "Django model"
- "Alembic migration" → "Django migration (`makemigrations`)"
- "async session" → removed
- "repository pattern" → "Django manager/queryset"

Phase-2 issues referencing FastAPI (#65, #66, #69, #72) need similar rewrites for Django/django-ninja.

## Issues to Close (summary)

| # | Title | Reason |
|---|-------|--------|
| 40 | Tenant model + RLS | Single-tenant, no tenants |
| 56 | Tenant context middleware | Single-tenant |
| 66 | JWT token management | Django sessions + API keys |
| 70 | RBAC engine | Django permissions IS this |
| 71 | ABAC policy engine | Premature |
| 92 | Domain event bus | Use Django signals |
| 131 | Multi-tenancy RLS | Single-tenant |
| 132 | Schema-per-tenant | Single-tenant |

## Validation

Cross-phase backward dependencies resolved by this plan:
- #131 (was phase-11, depended on by phase-0 #150) → Closed
- #136, #137, #140, #141 (phase-11 → phase-0 #150) → Moved to Wave 9, #150's deps updated
- #118 (phase-9 → phase-0 #17) → Moved to Wave 6, natural ordering

Stale technology references: 36 issues flagged, all addressed by rewrite or closure above.

Issue #150 (this roadmap issue) is meta — it is completed by this document and the `tools/issue-graph` tooling.
