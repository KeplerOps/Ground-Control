# Ground Control — Data Model & Storage Design

**Version:** 1.0.0
**Date:** 2026-03-07

---

## 1. Entity-Relationship Overview

```
┌──────────────┐       ┌──────────────┐       ┌──────────────────┐
│   Tenant     │1─────*│    User      │*─────*│     Role         │
└──────────────┘       └──────────────┘       └──────────────────┘
       │1                     │*                      │
       │                      │                       │
       │          ┌───────────┼───────────┐          │
       │          │           │           │          │
       ▼*         ▼*          ▼*          ▼*         │
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│   Risk   │ │ Control  │ │Assessment│ │  Agent  │ │
│          │ │          │ │ Campaign │ │         │ │
└──────────┘ └──────────┘ └──────────┘ └─────────┘ │
     │*           │*            │1                   │
     │            │             │                    │
     │     ┌──────┘             ▼*                   │
     │     │          ┌──────────────────┐          │
     │     │          │ Test Procedure   │          │
     │     │          └──────────────────┘          │
     │     │                   │*                    │
     │     │                   │                     │
     ▼*    ▼*                  ▼*                    │
┌─────────────────────────────────────────────┐    │
│              Artifact (Evidence)             │    │
└─────────────────────────────────────────────┘    │
     │*                        │*                   │
     │                         │                    │
     ▼*                        ▼*                   │
┌──────────┐          ┌──────────────┐             │
│  Finding │          │  Audit Log   │             │
│          │          │  Entry       │             │
└──────────┘          └──────────────┘             │
     │*                                             │
     ▼*                                             │
┌──────────────────┐                                │
│Remediation Plan  │                                │
└──────────────────┘                                │

Cross-cutting:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│    Framework     │  │ Framework        │  │ Common Control   │
│                  │*─│ Requirement      │*─│ Library Entry    │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 2. Core Entities

### 2.1 Tenant

```sql
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    slug            TEXT NOT NULL UNIQUE,
    settings        JSONB NOT NULL DEFAULT '{}',
    status          TEXT NOT NULL DEFAULT 'active',  -- active, suspended, archived
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.2 User

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    auth_provider   TEXT NOT NULL DEFAULT 'local',  -- local, saml, oidc
    external_id     TEXT,                            -- IdP subject identifier
    status          TEXT NOT NULL DEFAULT 'active',  -- active, inactive, suspended
    mfa_enabled     BOOLEAN NOT NULL DEFAULT false,
    settings        JSONB NOT NULL DEFAULT '{}',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, email)
);
```

### 2.3 Role & Permission

```sql
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            TEXT NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT false,  -- built-in roles can't be deleted
    permissions     JSONB NOT NULL DEFAULT '[]',      -- ["risks:read:*", "risks:write:bu=eng"]
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, name)
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users(id),
    role_id         UUID NOT NULL REFERENCES roles(id),
    scope           JSONB DEFAULT '{}',  -- optional: {"business_unit": "engineering"}
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by      UUID REFERENCES users(id),

    PRIMARY KEY (user_id, role_id)
);
```

### 2.4 Risk

```sql
CREATE TABLE risks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    ref_id              TEXT NOT NULL,             -- human-readable: RISK-001
    title               TEXT NOT NULL,
    description         TEXT,
    category            TEXT NOT NULL,             -- from taxonomy
    owner_id            UUID REFERENCES users(id),
    status              TEXT NOT NULL DEFAULT 'open',  -- open, mitigated, accepted, closed, archived

    -- Inherent risk scores
    inherent_likelihood INTEGER NOT NULL,
    inherent_impact     INTEGER NOT NULL,
    inherent_score      NUMERIC GENERATED ALWAYS AS (inherent_likelihood * inherent_impact) STORED,

    -- Residual risk scores
    residual_likelihood INTEGER,
    residual_impact     INTEGER,
    residual_score      NUMERIC GENERATED ALWAYS AS (residual_likelihood * residual_impact) STORED,

    -- Risk appetite
    appetite_threshold  NUMERIC,

    -- Metadata
    business_units      TEXT[] NOT NULL DEFAULT '{}',
    tags                TEXT[] NOT NULL DEFAULT '{}',
    custom_fields       JSONB NOT NULL DEFAULT '{}',

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at         TIMESTAMPTZ,

    UNIQUE (tenant_id, ref_id)
);

CREATE INDEX idx_risks_tenant_category ON risks(tenant_id, category);
CREATE INDEX idx_risks_tenant_owner ON risks(tenant_id, owner_id);
CREATE INDEX idx_risks_tenant_status ON risks(tenant_id, status);
```

### 2.5 Control

```sql
CREATE TABLE controls (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    ref_id              TEXT NOT NULL,             -- CTRL-AM-001
    ccl_entry_id        UUID,                      -- link to Common Control Library
    title               TEXT NOT NULL,
    objective           TEXT,
    description         TEXT,
    control_type        TEXT NOT NULL,             -- preventive, detective, corrective
    control_nature      TEXT NOT NULL,             -- manual, automated, it_dependent_manual
    frequency           TEXT NOT NULL,             -- continuous, daily, weekly, monthly, quarterly, annual, ad_hoc
    owner_id            UUID REFERENCES users(id),
    status              TEXT NOT NULL DEFAULT 'active',  -- active, retired, draft

    -- Effectiveness
    effectiveness_rating TEXT,                     -- effective, needs_improvement, ineffective
    last_tested_at       TIMESTAMPTZ,

    -- Metadata
    business_units      TEXT[] NOT NULL DEFAULT '{}',
    systems             TEXT[] NOT NULL DEFAULT '{}',
    tags                TEXT[] NOT NULL DEFAULT '{}',
    custom_fields       JSONB NOT NULL DEFAULT '{}',

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, ref_id)
);

CREATE INDEX idx_controls_tenant_type ON controls(tenant_id, control_type);
CREATE INDEX idx_controls_tenant_owner ON controls(tenant_id, owner_id);
```

### 2.6 Framework & Requirements

```sql
CREATE TABLE frameworks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id),  -- NULL = global/system framework
    name            TEXT NOT NULL,                 -- "SOX ITGC", "ISO 27001:2022"
    version         TEXT NOT NULL,
    description     TEXT,
    source_plugin   TEXT,                          -- plugin that provided this framework
    status          TEXT NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE framework_requirements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    framework_id    UUID NOT NULL REFERENCES frameworks(id),
    ref_id          TEXT NOT NULL,             -- "CC6.1", "A.8.3", "AC-2"
    title           TEXT NOT NULL,
    description     TEXT,
    parent_id       UUID REFERENCES framework_requirements(id),  -- hierarchy
    sort_order      INTEGER NOT NULL DEFAULT 0,
    metadata        JSONB NOT NULL DEFAULT '{}',

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (framework_id, ref_id)
);

-- Many-to-many: Controls ↔ Framework Requirements
CREATE TABLE control_framework_mappings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    control_id          UUID NOT NULL REFERENCES controls(id),
    requirement_id      UUID NOT NULL REFERENCES framework_requirements(id),
    mapping_type        TEXT NOT NULL DEFAULT 'satisfies',  -- satisfies, partially_satisfies, related
    notes               TEXT,
    suggested_by_agent  UUID,                    -- if AI-suggested, agent_id
    confidence          NUMERIC,                 -- agent confidence score
    approved_by         UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (control_id, requirement_id)
);
```

### 2.7 Common Control Library (CCL)

```sql
CREATE TABLE ccl_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id          TEXT NOT NULL UNIQUE,       -- "CC-AM-001"
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    category        TEXT NOT NULL,
    control_type    TEXT NOT NULL,
    control_nature  TEXT NOT NULL,
    version         INTEGER NOT NULL DEFAULT 1,
    status          TEXT NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- CCL entry ↔ Framework Requirements (reference mappings)
CREATE TABLE ccl_framework_mappings (
    ccl_entry_id    UUID NOT NULL REFERENCES ccl_entries(id),
    requirement_id  UUID NOT NULL REFERENCES framework_requirements(id),

    PRIMARY KEY (ccl_entry_id, requirement_id)
);
```

### 2.8 Assessment Campaign

```sql
CREATE TABLE assessment_campaigns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            TEXT NOT NULL,
    campaign_type   TEXT NOT NULL,             -- sox_itgc, soc2, iso27001, custom
    status          TEXT NOT NULL DEFAULT 'planning',  -- planning, active, review, finalized, archived
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    fieldwork_start DATE,
    fieldwork_end   DATE,
    scope_filter    JSONB NOT NULL DEFAULT '{}',  -- {"business_units": [...], "control_types": [...]}

    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalized_at    TIMESTAMPTZ
);
```

### 2.9 Test Procedure & Steps

```sql
CREATE TABLE test_procedures (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES assessment_campaigns(id),
    control_id          UUID NOT NULL REFERENCES controls(id),
    template_id         UUID,                  -- from template library
    title               TEXT NOT NULL,
    description         TEXT,
    status              TEXT NOT NULL DEFAULT 'not_started',
    -- not_started, in_progress, completed, review, approved
    assigned_to         UUID REFERENCES users(id),
    assigned_agent_id   UUID,                  -- if assigned to an agent
    reviewer_id         UUID REFERENCES users(id),

    -- Results
    conclusion          TEXT,                  -- effective, ineffective, not_tested
    agent_produced      BOOLEAN NOT NULL DEFAULT false,
    agent_confidence    NUMERIC,
    agent_provenance    JSONB,

    -- Sampling
    population_size     INTEGER,
    sample_size         INTEGER,
    sampling_method     TEXT,                  -- statistical, judgmental, haphazard

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ
);

CREATE TABLE test_steps (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    procedure_id        UUID NOT NULL REFERENCES test_procedures(id),
    step_number         INTEGER NOT NULL,
    instruction         TEXT NOT NULL,
    expected_result     TEXT,

    -- Tester fills in:
    actual_result       TEXT,
    conclusion          TEXT,                  -- pass, fail, na
    notes               TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (procedure_id, step_number)
);
```

### 2.10 Artifact (Evidence)

```sql
CREATE TABLE artifacts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    filename            TEXT NOT NULL,
    content_type        TEXT NOT NULL,
    size_bytes          BIGINT NOT NULL,
    storage_key         TEXT NOT NULL,          -- S3/MinIO object key
    sha256_hash         TEXT NOT NULL,          -- integrity verification
    version             INTEGER NOT NULL DEFAULT 1,
    parent_artifact_id  UUID REFERENCES artifacts(id),  -- previous version

    -- Metadata
    uploaded_by         UUID NOT NULL REFERENCES users(id),
    uploaded_by_agent   UUID,                   -- if collected by agent
    description         TEXT,
    tags                TEXT[] NOT NULL DEFAULT '{}',
    custom_fields       JSONB NOT NULL DEFAULT '{}',

    -- Encryption
    encryption_method   TEXT NOT NULL DEFAULT 'server_aes256',
    encryption_key_id   TEXT,

    -- Retention
    retention_until     DATE,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_artifacts_tenant ON artifacts(tenant_id);
CREATE INDEX idx_artifacts_hash ON artifacts(sha256_hash);

-- Many-to-many: Artifacts ↔ any entity
CREATE TABLE artifact_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id     UUID NOT NULL REFERENCES artifacts(id),
    entity_type     TEXT NOT NULL,             -- risk, control, test_procedure, test_step, finding
    entity_id       UUID NOT NULL,
    context_note    TEXT,                      -- why this evidence is relevant
    linked_by       UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (artifact_id, entity_type, entity_id)
);

CREATE INDEX idx_artifact_links_entity ON artifact_links(entity_type, entity_id);
```

### 2.11 Evidence Request

```sql
CREATE TABLE evidence_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    campaign_id     UUID REFERENCES assessment_campaigns(id),
    control_id      UUID REFERENCES controls(id),
    procedure_id    UUID REFERENCES test_procedures(id),

    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    format_guidance TEXT,
    due_date        DATE NOT NULL,
    status          TEXT NOT NULL DEFAULT 'pending',  -- pending, submitted, accepted, rejected, overdue

    requested_by    UUID NOT NULL REFERENCES users(id),
    assigned_to     UUID NOT NULL REFERENCES users(id),  -- control owner

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at    TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ
);
```

### 2.12 Finding

```sql
CREATE TABLE findings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    ref_id              TEXT NOT NULL,
    campaign_id         UUID REFERENCES assessment_campaigns(id),
    control_id          UUID REFERENCES controls(id),
    procedure_id        UUID REFERENCES test_procedures(id),

    title               TEXT NOT NULL,
    description         TEXT NOT NULL,
    root_cause          TEXT,
    risk_rating         TEXT NOT NULL,         -- high, medium, low
    classification      TEXT NOT NULL,         -- deficiency, significant_deficiency, material_weakness

    status              TEXT NOT NULL DEFAULT 'draft',
    -- draft, open, remediation_in_progress, validation, closed

    owner_id            UUID REFERENCES users(id),  -- remediation owner
    due_date            DATE,
    agent_produced      BOOLEAN NOT NULL DEFAULT false,

    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ,

    UNIQUE (tenant_id, ref_id)
);
```

### 2.13 Remediation Plan

```sql
CREATE TABLE remediation_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id      UUID NOT NULL REFERENCES findings(id),
    description     TEXT NOT NULL,
    owner_id        UUID NOT NULL REFERENCES users(id),
    target_date     DATE NOT NULL,
    status          TEXT NOT NULL DEFAULT 'planned',  -- planned, in_progress, completed, validated

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE remediation_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID NOT NULL REFERENCES remediation_plans(id),
    description     TEXT NOT NULL,
    owner_id        UUID REFERENCES users(id),
    due_date        DATE,
    status          TEXT NOT NULL DEFAULT 'pending',  -- pending, in_progress, completed
    sort_order      INTEGER NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);
```

### 2.14 Agent Registration

```sql
CREATE TABLE agents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            TEXT NOT NULL,
    description     TEXT,
    owner_id        UUID NOT NULL REFERENCES users(id),  -- human owner
    client_id       TEXT NOT NULL UNIQUE,
    client_secret_hash TEXT NOT NULL,           -- Argon2id hash
    role_id         UUID NOT NULL REFERENCES roles(id),
    status          TEXT NOT NULL DEFAULT 'active',  -- active, suspended, revoked
    allowed_scopes  TEXT[] NOT NULL DEFAULT '{}',

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_active_at  TIMESTAMPTZ
);
```

### 2.15 Audit Log

```sql
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id        UUID NOT NULL,
    actor_type      TEXT NOT NULL,             -- user, agent, system
    action          TEXT NOT NULL,             -- create, update, delete, login, approve, reject
    resource_type   TEXT NOT NULL,
    resource_id     UUID NOT NULL,
    changes         JSONB,                    -- {"field": {"old": "...", "new": "..."}}
    ip_address      INET,
    user_agent      TEXT,
    previous_hash   TEXT,                     -- chain for tamper detection
    entry_hash      TEXT NOT NULL              -- SHA-256 of this entry + previous_hash
);

-- Append-only: no UPDATE or DELETE allowed (enforced by trigger or policy)
CREATE INDEX idx_audit_log_tenant_ts ON audit_log(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id, timestamp DESC);
```

### 2.16 Risk Treatment Plan

```sql
CREATE TABLE risk_treatments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_id         UUID NOT NULL REFERENCES risks(id),
    treatment_type  TEXT NOT NULL,             -- accept, mitigate, transfer, avoid
    description     TEXT NOT NULL,
    owner_id        UUID REFERENCES users(id),
    status          TEXT NOT NULL DEFAULT 'planned',  -- planned, in_progress, completed
    target_date     DATE,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE treatment_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    treatment_id    UUID NOT NULL REFERENCES risk_treatments(id),
    description     TEXT NOT NULL,
    owner_id        UUID REFERENCES users(id),
    due_date        DATE,
    status          TEXT NOT NULL DEFAULT 'pending',
    sort_order      INTEGER NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);
```

### 2.17 Taxonomy Configuration

```sql
CREATE TABLE taxonomy_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    taxonomy_type   TEXT NOT NULL,             -- risk_category, control_type, control_nature, likelihood, impact, etc.
    value           TEXT NOT NULL,
    label           TEXT NOT NULL,
    description     TEXT,
    color           TEXT,                      -- hex color for UI
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, taxonomy_type, value)
);
```

### 2.18 Plugin Registration

```sql
CREATE TABLE plugins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    plugin_name     TEXT NOT NULL,
    version         TEXT NOT NULL,
    description     TEXT,
    author          TEXT,
    plugin_type     TEXT NOT NULL,             -- framework, integration, evidence_collector, workflow, report, agent
    status          TEXT NOT NULL DEFAULT 'installed',  -- installed, enabled, disabled, error
    config          JSONB NOT NULL DEFAULT '{}',
    permissions     TEXT[] NOT NULL DEFAULT '{}',
    signature       TEXT,                      -- Ed25519 signature
    health_status   TEXT DEFAULT 'unknown',

    installed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, plugin_name)
);
```

### 2.19 Notification & Comment

```sql
CREATE TABLE comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    entity_type     TEXT NOT NULL,
    entity_id       UUID NOT NULL,
    parent_id       UUID REFERENCES comments(id),  -- for threading
    author_id       UUID NOT NULL REFERENCES users(id),
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_entity ON comments(entity_type, entity_id);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    type            TEXT NOT NULL,             -- assignment, deadline, review_request, evidence_request, etc.
    title           TEXT NOT NULL,
    body            TEXT,
    entity_type     TEXT,
    entity_id       UUID,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);
```

---

## 3. Relationship Summary (ERD Legend)

```
Tenant          1──*  User, Risk, Control, Assessment Campaign, Finding, Artifact, Agent, Plugin
User            *──*  Role (via user_roles)
Risk            *──*  Control (via risk_control_mappings — optional)
Risk            1──*  Risk Treatment
Control         *──*  Framework Requirement (via control_framework_mappings)
Control         *──1  CCL Entry (optional reference)
CCL Entry       *──*  Framework Requirement (via ccl_framework_mappings)
Framework       1──*  Framework Requirement
Assessment      1──*  Test Procedure
Test Procedure  1──*  Test Step
Test Procedure  *──1  Control
Artifact        *──*  Any Entity (via artifact_links, polymorphic)
Finding         *──1  Campaign, Control, Test Procedure
Finding         1──*  Remediation Plan
Remediation Plan 1──* Remediation Action
Comment         *──1  Any Entity (polymorphic)
Audit Log       *──1  Any Entity (polymorphic, append-only)
```

---

## 4. Storage Strategy

### 4.1 Structured Data — PostgreSQL

- All entities above reside in PostgreSQL.
- JSONB columns (`custom_fields`, `settings`, `config`, `changes`) provide schema flexibility without sacrificing query performance.
- Row-Level Security (RLS) policies enforce tenant isolation at the database level.

```sql
-- Example RLS policy
ALTER TABLE risks ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON risks
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

### 4.2 Binary Artifacts — S3-Compatible Object Store

- Artifacts stored under: `{tenant_id}/{year}/{month}/{artifact_id}/{version}/{filename}`
- Server-side encryption (SSE-S3 or SSE-KMS).
- Pre-signed URLs for direct browser upload/download (bypass API server for large files).
- Lifecycle policies handle retention (move to Glacier/cold after retention window).

### 4.3 Search Index — Meilisearch

Indexed entities:
- Risks (title, description, category, tags)
- Controls (title, objective, description, ref_id)
- Findings (title, description)
- Artifacts (filename, description, tags)
- Framework Requirements (ref_id, title, description)

### 4.4 Caching — Redis/Valkey

Cached objects:
- User sessions and JWT validation cache
- Dashboard aggregations (TTL: 60s)
- Taxonomy lookups (TTL: 300s)
- Rate limit counters

---

## 5. Migration Strategy

All schema changes managed via **Alembic** migrations with:
- Forward and rollback scripts for every migration
- Data migrations for taxonomy or framework updates
- Zero-downtime migration patterns (add column → backfill → add constraint)
