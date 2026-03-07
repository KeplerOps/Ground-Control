# Ground Control — Product Requirements Document (PRD)

**Version:** 1.0.0
**Date:** 2026-03-07
**Status:** Draft

---

## 1. Executive Summary

Ground Control is an open, self-hostable IT Risk Management (ITRM) platform
that replaces proprietary GRC tools such as AuditBoard ITRM, ServiceNow GRC,
and Archer. It delivers a modern, API-first, plugin-extensible system for
managing IT risk assessments, control testing, evidence collection, and
compliance reporting.

The platform is designed for internal audit teams, IT risk managers, compliance
analysts, and — critically — AI agents that can perform assessments alongside
human practitioners. Every workflow, object, and report is accessible through a
versioned API and extensible via a plugin system.

### 1.1 Problem Statement

Organizations using AuditBoard ITRM and similar tools face recurring pain points:

| Pain Point | Detail |
|---|---|
| **Vendor lock-in** | SaaS-only deployment, no self-hosting, data residency concerns |
| **Rigid workflows** | Hard-coded assessment flows that don't match org-specific processes |
| **Poor API coverage** | Limited automation; manual CSV exports for integration |
| **No agent support** | No first-class path for AI/automation agents to perform assessments |
| **Evidence sprawl** | Artifacts scattered across email, SharePoint, tickets with weak lineage |
| **Framework silos** | Each framework (SOX, SOC 2, ISO 27001) maintained separately despite overlapping controls |
| **Expensive seats** | Per-user pricing that discourages broad organizational participation |
| **Opaque taxonomy** | Each team invents its own risk and control language; no shared library |

### 1.2 Vision

Ground Control consolidates IT risks, controls, and evidence into a single
system with full lineage, accessible to both humans and AI agents, self-hostable,
and extensible via plugins.

---

## 2. Target Users & Personas

### P1 — IT Risk Manager (Riley)
- Owns the IT risk register. Runs annual and continuous risk assessments.
- Needs: heat maps, risk scoring, treatment tracking, board-ready reports.

### P2 — Internal Auditor (Avery)
- Plans and executes ITGC audits (SOX, SOC 2). Tests controls, collects evidence.
- Needs: workpapers, test procedures, evidence linking, review workflows.

### P3 — Control Owner (Jordan)
- Business/IT staff responsible for operating a control (e.g., access reviews).
- Needs: simple task list, evidence upload, status updates, reminders.

### P4 — Compliance Analyst (Morgan)
- Maps controls to multiple frameworks, tracks regulatory changes.
- Needs: cross-framework mapping, gap analysis, regulation tracking.

### P5 — CISO / Risk Committee (Pat)
- Consumes dashboards and reports. Makes resource/accept/mitigate decisions.
- Needs: executive dashboards, trend analysis, risk appetite visualization.

### P6 — AI Assessment Agent
- Automated agent that performs control testing, evidence analysis, risk scoring.
- Needs: full API access, structured inputs/outputs, audit trail of agent actions.

### P7 — Platform Administrator (Sam)
- Configures the platform: SSO, roles, plugins, integrations.
- Needs: admin console, SCIM provisioning, plugin marketplace, audit logs.

---

## 3. Frameworks & Standards Supported (Out of Box)

| Framework | Coverage |
|---|---|
| SOX ITGC | Full — control objectives, test procedures, walkthroughs, deficiency classification |
| SOC 2 (Trust Services Criteria) | Full — all five categories with pre-built control mappings |
| ISO 27001:2022 | Full — Annex A controls, Statement of Applicability generation |
| NIST CSF 2.0 | Full — all six functions, categories, subcategories |
| NIST SP 800-53 Rev 5 | Full — control families with enhancements |
| COBIT 2019 | Core — governance and management objectives |
| PCI-DSS v4.0 | Full — requirements, testing procedures, compensating controls |
| CIS Controls v8 | Full — safeguards mapped to implementation groups |
| GDPR (Article 32+) | Supplemental — data protection controls overlay |
| HIPAA Security Rule | Supplemental — administrative, physical, technical safeguards |
| Custom Frameworks | User-defined frameworks loaded via plugin |

**Cross-Framework Mapping:** The platform maintains a unified Common Control
Library (CCL). Each common control maps to one or more framework-specific
requirements, enabling "test once, comply many" workflows.

---

## 4. Core Capabilities

### 4.1 Risk Management

- **Risk Register** — Centralized catalog of IT risks with customizable taxonomy
  (categories, likelihood scales, impact scales, risk appetite thresholds).
- **Risk Assessment Campaigns** — Time-boxed assessment cycles with configurable
  scoring methodologies (quantitative, qualitative, semi-quantitative).
- **Risk Scoring Engine** — Pluggable scoring models. Default: 5x5 inherent /
  residual matrix. Supports FAIR-based quantitative analysis via plugin.
- **Risk Treatment Plans** — Accept, mitigate, transfer, avoid — with linked
  action items, owners, and due dates.
- **Heat Maps & Dashboards** — Interactive risk heat maps, trend lines, appetite
  overlays.
- **Continuous Risk Monitoring** — Ingest risk indicators from external sources
  (vulnerability scanners, cloud posture tools) via API/plugins.

### 4.2 Control Management

- **Common Control Library (CCL)** — Reusable control definitions with standard
  language, mapped across frameworks.
- **Control Catalog** — Organization-specific controls linked to CCL entries.
  Each control has: objective, description, type (preventive/detective/corrective),
  nature (manual/automated/IT-dependent manual), frequency, owner.
- **Control-to-Framework Mapping** — Many-to-many mapping. A single control can
  satisfy SOX ITGC 4.1, ISO 27001 A.8.3, and NIST AC-2 simultaneously.
- **Control Effectiveness Rating** — Configurable rating scales with evidence-based
  justification.

### 4.3 Assessment & Testing

- **Assessment Campaigns** — Plan, assign, execute, review, and close assessment
  cycles (e.g., "Q1 2026 SOX ITGC Testing").
- **Test Procedures** — Templated or ad-hoc test steps. Each step captures:
  expected result, actual result, evidence references, conclusion.
- **Workpapers** — Structured workpaper documents with sections, findings,
  and review sign-off chains.
- **Sampling** — Configurable sampling methodologies (statistical, judgmental,
  haphazard) with sample-size calculators.
- **Agent-Performed Testing** — AI agents can execute test procedures via API,
  attach results, and flag items for human review. Every agent action is logged
  with full provenance (agent ID, model, prompt hash, timestamp).

### 4.4 Evidence & Artifact Management

- **Artifact Store** — Upload, version, tag, and retrieve any file type.
  Server-side encryption at rest (AES-256). Client-side encryption option.
- **Evidence Linking** — Link artifacts to controls, risks, test steps, findings,
  or any other entity. Many-to-many with metadata.
- **Evidence Requests** — Send structured requests to control owners with
  deadlines and reminders. Owners upload directly; no email attachments.
- **Automated Evidence Collection** — Plugins pull evidence from source systems
  (AWS Config, Azure Policy, Jira, ServiceNow, GitHub) on schedule or trigger.
- **Evidence Lineage** — Full chain of custody: who uploaded, when, which
  assessment it was used in, who reviewed it, hash integrity verification.
- **Retention Policies** — Configurable per-assessment or global retention
  windows with automated archival/deletion.

### 4.5 Findings & Issues

- **Finding Lifecycle** — Draft → Open → Remediation In Progress → Validation
  → Closed. Fully configurable states.
- **Deficiency Classification** — SOX-aligned: deficiency, significant deficiency,
  material weakness. Customizable for other frameworks.
- **Remediation Tracking** — Action plans with owners, due dates, evidence of
  remediation, and validation testing.
- **Issue Aggregation** — Roll up findings from multiple assessments into a
  unified issues view with risk ratings.

### 4.6 Reporting & Dashboards

- **Executive Dashboards** — Risk posture, control health, assessment progress,
  finding trends.
- **Board Reports** — Generate PDF summary reports.
- **Custom Reports** — Filterable, exportable report views.
- **Scheduled Reports** — Email delivery on configurable schedules.
- **API-Driven Analytics** — All report data available via API for BI tool
  integration (Tableau, Power BI, Looker).

### 4.7 Workflow & Collaboration

- **Review Workflows** — Configurable review chains (e.g., preparer → reviewer → approver).
- **Notifications** — In-app, email, Slack/Teams webhooks. Configurable per
  user and per event type.
- **Comments & Annotations** — Threaded comments on any entity. @-mention
  users or groups.
- **Task Assignment** — Assign any work item to users or groups with due dates
  and SLA tracking.

---

## 5. Agent-First Design

### 5.1 Agent Interaction Model

Agents are first-class actors in Ground Control. They authenticate via API keys
or OAuth2 client credentials and interact through the same API as humans.

### 5.2 Agent Capabilities

| Capability | Description |
|---|---|
| **Execute Test Procedures** | Agent receives structured test steps, performs analysis, returns structured results |
| **Analyze Evidence** | Agent ingests uploaded artifacts, extracts relevant data, flags anomalies |
| **Score Risks** | Agent applies scoring models to risk data, provides quantitative assessments |
| **Draft Findings** | Agent produces finding drafts from test results for human review |
| **Map Controls** | Agent suggests framework mappings for new controls based on CCL |
| **Monitor Continuously** | Agent polls external systems and updates risk indicators |

### 5.3 Agent Provenance

Every action by an agent records:
- Agent identity (registered agent ID, human owner)
- Model/version used
- Input context hash (for reproducibility)
- Confidence score (where applicable)
- Human review status (pending / approved / rejected)

---

## 6. Common Language & Reusability

### 6.1 Shared Taxonomy

Ground Control enforces a configurable but consistent taxonomy:

- **Risk Categories** — e.g., Access Management, Change Management, Operations,
  Data Protection, Third Party, Business Continuity.
- **Control Types** — Preventive, Detective, Corrective.
- **Control Nature** — Manual, Automated, IT-Dependent Manual.
- **Likelihood Scales** — Configurable 3/4/5-point scales with org-defined labels.
- **Impact Scales** — Configurable with financial, reputational, regulatory dimensions.
- **Rating Scales** — Effective, Needs Improvement, Ineffective (configurable).

### 6.2 Common Control Library (CCL)

The CCL is the heart of reusability:

```
┌──────────────────────────────────────────────────┐
│  Common Control: CC-AM-001                        │
│  "Logical access to systems is restricted to      │
│   authorized users based on business need."       │
│                                                   │
│  Maps to:                                         │
│  ├── SOX ITGC: Access to Programs and Data        │
│  ├── SOC 2: CC6.1                                 │
│  ├── ISO 27001: A.8.3, A.5.15                     │
│  ├── NIST 800-53: AC-2, AC-3                      │
│  └── PCI-DSS: 7.1, 7.2                           │
│                                                   │
│  Test once → evidence satisfies all five frameworks│
└──────────────────────────────────────────────────┘
```

### 6.3 Template Library

- **Assessment Templates** — Pre-built assessment structures for common scopes.
- **Test Procedure Templates** — Reusable test procedures linked to CCL controls.
- **Workpaper Templates** — Standard workpaper formats with required sections.
- **Finding Templates** — Boilerplate finding language by deficiency type.
- **Report Templates** — Customizable report formats.

All templates are versionable, exportable, and shareable between tenants.

---

## 7. Non-Functional Requirements

| Requirement | Target |
|---|---|
| **Availability** | No single points of failure in the application layer; infrastructure HA depends on deployment topology |
| **Response Time** | API p95 < 200ms for CRUD operations; < 2s for reports |
| **Concurrent Users** | Design for horizontal scaling; no hard-coded concurrency limits |
| **Data Encryption** | AES-256 at rest, TLS 1.3 in transit |
| **Audit Logging** | Immutable audit log for every state change |
| **Multi-Tenancy** | Full tenant isolation (schema-per-tenant or DB-per-tenant) |
| **Accessibility** | WCAG 2.1 AA compliant |
| **Internationalization** | UTF-8 throughout; UI string externalization for i18n |
| **Backup/Recovery** | RPO < 1 hour, RTO < 4 hours |
| **Max Artifact Size** | 500 MB per file; configurable per tenant |

---

## 8. Integration Requirements

### 8.1 Authentication & Identity

| Method | Details |
|---|---|
| SAML 2.0 | SP-initiated and IdP-initiated SSO |
| OpenID Connect (OIDC) | Authorization Code flow with PKCE |
| SCIM 2.0 | Automated user/group provisioning and de-provisioning |
| API Keys | Long-lived keys for service accounts and agents |
| OAuth2 Client Credentials | Machine-to-machine authentication for agents |
| MFA | TOTP and WebAuthn support for local accounts |

### 8.2 External System Integrations (via Plugins)

| Category | Examples |
|---|---|
| Ticketing | Jira, ServiceNow, Azure DevOps |
| Cloud Posture | AWS Config/SecurityHub, Azure Policy/Defender, GCP SCC |
| Identity | Okta, Azure AD/Entra ID, Ping Identity |
| Source Control | GitHub, GitLab, Bitbucket (for change management evidence) |
| Communication | Slack, Microsoft Teams, Email (SMTP) |
| BI/Analytics | Tableau, Power BI, Looker (via API or direct DB read replica) |
| Vulnerability | Qualys, Tenable, Rapid7 |
| CMDB | ServiceNow CMDB, Device42, Snipe-IT |

---

## 9. Success Metrics

| Metric | Target |
|---|---|
| API coverage | All core workflows (risk, control, assessment, evidence, finding) accessible via API at v0.1 |
| Agent automation | At least one end-to-end agent workflow (test procedure execution) functional at v0.4 |
| Cross-framework mapping | CCL supports SOX ITGC, SOC 2, and ISO 27001 with shared controls at v0.3 |
| Self-host deployment | Single-command Docker Compose deployment with < 15 minutes to first login |
| Test coverage | Backend unit test coverage > 80%; all API endpoints have integration tests |

---

## 10. Release Roadmap

### v0.1 — Foundation (MVP)
- Core data model (risks, controls, assessments, evidence, findings)
- REST API with OpenAPI spec
- Basic web UI (risk register, control catalog, assessment execution)
- Local auth + OIDC SSO
- File-based artifact storage
- SQLite/PostgreSQL backend
- MCP server integration (rocq-mcp for formal proofs, AWS MCP for infrastructure)

### v0.2 — Collaboration
- Review workflows and approval chains
- Comments and notifications
- Evidence requests
- SAML 2.0 SSO + SCIM provisioning

### v0.3 — Frameworks & Mapping
- Pre-built framework libraries (SOX, SOC 2, ISO 27001, NIST)
- Common Control Library with cross-mapping
- Template library (assessments, test procedures, workpapers)

### v0.4 — Agents & Automation
- Agent authentication and provenance tracking
- Agent SDK (Python, TypeScript)
- Automated evidence collection plugins
- Continuous monitoring hooks

### v0.5 — Reporting & Analytics
- Executive dashboards
- Custom report builder
- Scheduled report delivery

### v1.0 — Production Ready
- Multi-tenancy
- Plugin system (install from local packages)
- Full RBAC with ABAC policies
- Kubernetes Helm chart and Docker Compose
- Comprehensive documentation and certification guide

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Scope creep from framework complexity | High | Start with SOX ITGC + SOC 2; add frameworks via plugins |
| Agent trust and reliability | High | Mandatory human review for agent-produced findings; confidence thresholds |
| Data migration from AuditBoard | Medium | Build import adapters for CSV/API export formats |
| Plugin security | Medium | Sandboxed plugin runtime; signed plugin packages |
| Self-hosting operational burden | Medium | Provide Helm charts, Terraform modules, and runbooks |

---

## Appendix A: Glossary

| Term | Definition |
|---|---|
| **CCL** | Common Control Library — reusable control definitions mapped across frameworks |
| **ITGC** | IT General Controls — foundational controls over IT environments |
| **GRC** | Governance, Risk, and Compliance |
| **ITRM** | IT Risk Management |
| **Assessment Campaign** | A time-boxed cycle of evaluating controls or risks |
| **Evidence** | Any artifact that demonstrates a control is operating effectively |
| **Finding** | An identified gap or deficiency in a control or process |
| **Workpaper** | A structured document that records testing procedures and results |
| **Tenant** | An isolated organizational unit within the platform |
| **Agent** | An AI/automation system that performs assessment tasks via API |
