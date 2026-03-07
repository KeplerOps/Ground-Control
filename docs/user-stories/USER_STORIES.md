# Ground Control — User Stories

**Version:** 1.0.0
**Date:** 2026-03-07

Stories are grouped by epic and reference personas from the PRD.

---

## Epic 1: Risk Management

### US-1.1 — Maintain Risk Register [MVP]
**As** Riley (IT Risk Manager),
**I want** to create, view, edit, and archive IT risks in a centralized register,
**So that** the organization has a single source of truth for its IT risk landscape.

**Acceptance Criteria:**
- [ ] Can create a risk with: title, description, category, owner, inherent likelihood, inherent impact
- [ ] Can assign a risk to one or more business units / systems
- [ ] Can filter and search risks by category, owner, rating, status
- [ ] Can archive a risk (soft-delete) with a reason
- [ ] Risk history (all field changes) is preserved in an immutable audit log
- [ ] Bulk import risks from CSV or JSON

### US-1.2 — Conduct Risk Assessment Campaign
**As** Riley,
**I want** to launch a time-boxed risk assessment campaign that assigns risks to assessors,
**So that** the organization periodically reassesses its risk posture.

**Acceptance Criteria:**
- [ ] Can create a campaign with: name, scope (risk categories, business units), start/end dates
- [ ] Can assign individual risks to specific assessors
- [ ] Assessors receive notifications and see assigned risks in their task list
- [ ] Assessors can update likelihood and impact scores with justification text and evidence links
- [ ] Campaign dashboard shows completion percentage, overdue items, score changes
- [ ] Campaign can be finalized (locked) after review

### US-1.3 — Define Risk Appetite & Thresholds
**As** Pat (CISO),
**I want** to set risk appetite thresholds that visually overlay on heat maps,
**So that** the organization can see which risks exceed tolerance levels.

**Acceptance Criteria:**
- [ ] Can define appetite thresholds per risk category (or global)
- [ ] Heat map displays appetite boundary line
- [ ] Risks above appetite are flagged and trigger notifications
- [ ] Can export heat map with appetite overlay as image or PDF

### US-1.4 — Track Risk Treatment Plans
**As** Riley,
**I want** to create treatment plans linked to risks with action items, owners, and deadlines,
**So that** risk mitigation activities are tracked to completion.

**Acceptance Criteria:**
- [ ] Can create a treatment plan (accept, mitigate, transfer, avoid) linked to a risk
- [ ] Each plan has one or more action items with owner, due date, status
- [ ] Owners receive reminders as due dates approach
- [ ] Completing all action items prompts residual risk re-assessment
- [ ] Treatment plan history is auditable

### US-1.5 — View Risk Dashboard
**As** Pat (CISO),
**I want** an executive dashboard showing overall risk posture, trends, and top risks,
**So that** I can make informed decisions in risk committee meetings.

**Acceptance Criteria:**
- [ ] Dashboard shows: risk heat map, top 10 risks, trend over last 4 quarters, treatment plan status
- [ ] Can filter by business unit, category, assessment period
- [ ] Can export dashboard as PDF for board reporting
- [ ] Dashboard data refreshes on page load

---

## Epic 2: Control Management

### US-2.1 — Maintain Control Catalog [MVP]
**As** Avery (Internal Auditor),
**I want** to maintain a catalog of IT controls with standard attributes,
**So that** controls are consistently documented and testable.

**Acceptance Criteria:**
- [ ] Can create a control with: ID, title, objective, description, type, nature, frequency, owner
- [ ] Controls link to the Common Control Library (CCL)
- [ ] Can map a control to one or more framework requirements
- [ ] Can filter controls by framework, type, nature, owner, effectiveness rating
- [ ] Control changes are version-controlled with diff history

### US-2.2 — Map Controls Across Frameworks
**As** Morgan (Compliance Analyst),
**I want** to map a single control to multiple framework requirements,
**So that** testing that control once satisfies compliance obligations across frameworks.

**Acceptance Criteria:**
- [ ] Can search framework requirements by ID or keyword
- [ ] Can link a control to multiple framework requirements (many-to-many)
- [ ] Mapping view shows which frameworks are covered for each control
- [ ] Gap analysis report shows framework requirements without mapped controls
- [ ] Can bulk-map controls using CCL suggested mappings

### US-2.3 — Use Common Control Library
**As** Morgan,
**I want** to browse and adopt pre-built common controls from the CCL,
**So that** I can quickly establish control coverage using standardized language.

**Acceptance Criteria:**
- [ ] Can browse CCL by category, keyword, or mapped framework
- [ ] Can adopt a CCL control into the org catalog (creates a linked copy)
- [ ] Adopted controls inherit framework mappings from the CCL
- [ ] When CCL updates, adopted controls show available updates
- [ ] Can contribute custom controls back to a shared CCL (if configured)

---

## Epic 3: Assessment & Testing

### US-3.1 — Plan Assessment Campaign [MVP]
**As** Avery,
**I want** to create an assessment campaign that defines scope, timeline, and team assignments,
**So that** testing work is organized and tracked.

**Acceptance Criteria:**
- [ ] Can create a campaign with: name, type (SOX ITGC, SOC 2, etc.), period, scope (controls/systems)
- [ ] Can assign testers and reviewers to specific controls
- [ ] Campaign generates workpapers from templates for each in-scope control
- [ ] Can set milestones (fieldwork start, fieldwork end, review deadline, final report)
- [ ] Calendar view shows all campaigns and milestones

### US-3.2 — Execute Test Procedures [MVP]
**As** Avery,
**I want** to execute test procedures against a control and record results step by step,
**So that** testing is thorough, consistent, and documented.

**Acceptance Criteria:**
- [ ] Each control in a campaign has one or more test procedures (from template or ad-hoc)
- [ ] Each test procedure has ordered steps with: instruction, expected result
- [ ] Tester records: actual result, pass/fail/N-A, evidence links, notes
- [ ] Can select a sample from a population and record sample items
- [ ] Can mark a test procedure as complete, which rolls up to control status
- [ ] Partially completed test procedures show progress percentage

### US-3.3 — Collect Evidence via Requests
**As** Avery,
**I want** to send evidence requests to control owners with clear instructions and deadlines,
**So that** evidence collection is structured and tracked rather than ad-hoc.

**Acceptance Criteria:**
- [ ] Can create an evidence request linked to a control, test procedure, or assessment
- [ ] Request specifies: description of needed evidence, format guidance, due date
- [ ] Control owner receives notification and sees request in their portal
- [ ] Owner uploads evidence directly; artifacts auto-link to the request
- [ ] Overdue requests trigger escalation notifications
- [ ] Requester can approve or reject submitted evidence with comments

### US-3.4 — Review and Approve Workpapers
**As** Avery (as reviewer),
**I want** to review completed workpapers and either approve or send back for rework,
**So that** testing quality is maintained through peer/manager review.

**Acceptance Criteria:**
- [ ] Workpapers follow a configurable review chain (preparer → reviewer → approver)
- [ ] Reviewer can add review notes at workpaper or step level
- [ ] Reviewer can approve, reject (with comments), or request changes
- [ ] Approval locks the workpaper from further edits (unless re-opened)
- [ ] Review status is visible on the campaign dashboard

### US-3.5 — Agent-Performed Testing
**As** an AI Assessment Agent,
**I want** to receive assigned test procedures via API, execute them, and submit structured results,
**So that** routine tests can be automated while maintaining audit quality.

**Acceptance Criteria:**
- [ ] Agent authenticates via OAuth2 client credentials or API key
- [ ] Agent retrieves assigned test procedures with all context (control description, prior results, evidence)
- [ ] Agent submits results in structured format: step results, evidence references, confidence score
- [ ] Agent results are flagged as "agent-produced" and routed to human review by default
- [ ] Agent provenance metadata (model, version, input hash) is recorded
- [ ] Agent cannot mark a workpaper as final — only humans can approve

---

## Epic 4: Evidence & Artifact Management

### US-4.1 — Upload and Manage Artifacts [MVP]
**As** Jordan (Control Owner),
**I want** to upload files as evidence and have them securely stored with version tracking,
**So that** evidence is preserved with integrity for audit purposes.

**Acceptance Criteria:**
- [ ] Can upload files up to 500 MB (configurable) via UI or API
- [ ] Files are encrypted at rest (AES-256) and integrity-hashed (SHA-256)
- [ ] Can upload new versions of an artifact; previous versions are retained
- [ ] Can tag artifacts with metadata (type, period, control, system)
- [ ] Can preview common file types in-browser (PDF, images, text, spreadsheets)

### US-4.2 — Link Evidence to Entities [MVP]
**As** Avery,
**I want** to link an artifact to one or more controls, test steps, risks, or findings,
**So that** evidence relationships are explicit and traceable.

**Acceptance Criteria:**
- [ ] Can link an artifact to any entity type (many-to-many)
- [ ] Entity detail pages show linked artifacts
- [ ] Artifact detail page shows all linked entities
- [ ] Can add a description/context note to each link (why this evidence is relevant)
- [ ] Unlinking preserves audit trail of the former link

### US-4.3 — Automated Evidence Collection
**As** Avery,
**I want** to configure plugins that automatically collect evidence from source systems,
**So that** evidence gathering requires less manual effort.

**Acceptance Criteria:**
- [ ] Can configure a collection plugin (e.g., AWS Config snapshot, Jira query, GitHub PR list)
- [ ] Plugin runs on schedule or on-demand
- [ ] Collected artifacts are auto-linked to the configured control/assessment
- [ ] Collection runs are logged with status, timestamp, artifact count
- [ ] Failed collections generate alerts

### US-4.4 — Evidence Lineage and Chain of Custody
**As** an External Auditor (consumer),
**I want** to see the full history of an evidence artifact — who provided it, when, what it was linked to, and who reviewed it,
**So that** I can trust the integrity of the evidence.

**Acceptance Criteria:**
- [ ] Artifact detail shows full timeline: uploaded, linked, reviewed, approved
- [ ] Each event records: actor (user or agent), timestamp, action, context
- [ ] Hash verification confirms artifact has not been tampered with
- [ ] Can generate a chain-of-custody report for a set of artifacts

---

## Epic 5: Findings & Remediation

### US-5.1 — Record Findings [MVP]
**As** Avery,
**I want** to create findings when control testing identifies deficiencies,
**So that** gaps are formally documented and tracked to remediation.

**Acceptance Criteria:**
- [ ] Can create a finding linked to a control, test procedure, and assessment
- [ ] Finding includes: title, description, root cause, risk rating, classification (deficiency / significant deficiency / material weakness)
- [ ] Can attach evidence that supports the finding
- [ ] Finding follows configurable lifecycle states (Draft → Open → Remediation → Validation → Closed)
- [ ] Duplicate finding detection suggests potential matches

### US-5.2 — Manage Remediation
**As** Jordan (Control Owner),
**I want** to create and track remediation action plans for findings assigned to me,
**So that** I can resolve identified issues and demonstrate closure.

**Acceptance Criteria:**
- [ ] Finding shows assigned remediation owner and target date
- [ ] Owner creates action plan with steps, dates, and evidence of remediation
- [ ] Owner uploads remediation evidence and marks plan as complete
- [ ] Completion triggers validation testing by the audit team
- [ ] Overdue remediations trigger escalation notifications

### US-5.3 — Validate Remediation
**As** Avery,
**I want** to validate that remediation actions effectively address the finding,
**So that** findings are only closed when the issue is truly resolved.

**Acceptance Criteria:**
- [ ] Validator reviews remediation evidence and performs re-testing
- [ ] Can approve (close finding) or reject (send back for rework)
- [ ] Validation results and evidence are recorded on the finding
- [ ] Closed findings contribute to control effectiveness ratings

---

## Epic 6: Reporting

### US-6.1 — Generate Executive Reports
**As** Pat (CISO),
**I want** to generate board-ready reports summarizing risk posture, control health, and findings,
**So that** I can present to the risk committee and board of directors.

**Acceptance Criteria:**
- [ ] One-click report generation for standard report types (risk summary, control health, assessment status)
- [ ] Reports include charts, tables, and narrative sections
- [ ] Export as PDF or PPTX
- [ ] Can customize report templates (add logo, sections, boilerplate)
- [ ] Report archives are retained for historical comparison

### US-6.2 — Build Custom Reports
**As** Riley,
**I want** to build ad-hoc reports by selecting fields, filters, and groupings,
**So that** I can answer specific questions about the risk and control data.

**Acceptance Criteria:**
- [ ] Report view with selectable entities, filters, and export (risks, controls, findings, assessments)
- [ ] Can add filters, sort, group, and aggregate
- [ ] Can save reports as templates for reuse
- [ ] Export to CSV, Excel, PDF
- [ ] Can schedule saved reports for periodic delivery via email

### US-6.3 — API-Driven Analytics
**As** a BI Engineer,
**I want** to query Ground Control data via API or read replica,
**So that** I can build dashboards in our organization's BI tool.

**Acceptance Criteria:**
- [ ] REST and GraphQL APIs expose all reporting data
- [ ] Can configure a read-only database replica for direct BI tool connection
- [ ] API supports pagination, filtering, and field selection
- [ ] Rate limiting protects production workloads from heavy BI queries

---

## Epic 7: Administration & Platform

### US-7.1 — Configure SSO
**As** Sam (Platform Admin),
**I want** to configure SSO via SAML 2.0 or OIDC so users authenticate through our corporate IdP,
**So that** access is centrally managed and secure.

**Acceptance Criteria:**
- [ ] Admin UI to configure SAML 2.0 SP settings (entity ID, ACS URL, certificate)
- [ ] Admin UI to configure OIDC settings (issuer, client ID/secret, scopes)
- [ ] Can test SSO configuration before enforcing
- [ ] Can enforce SSO (disable local password login)
- [ ] JIT (just-in-time) provisioning creates user accounts on first SSO login

### US-7.2 — Manage Users and Roles [MVP]
**As** Sam,
**I want** to manage users, groups, and role assignments with fine-grained permissions,
**So that** users only access what they need.

**Acceptance Criteria:**
- [ ] Pre-built roles: Admin, Risk Manager, Auditor, Control Owner, Viewer, Agent
- [ ] Can create custom roles with granular permissions (per entity type: create, read, update, delete, approve)
- [ ] Can assign roles at global, business unit, or assessment scope
- [ ] SCIM 2.0 provisioning syncs users and groups from IdP
- [ ] User deactivation revokes access immediately; data is preserved

### US-7.3 — Install and Configure Plugins
**As** Sam,
**I want** to install plugins that extend Ground Control with new integrations and framework support,
**So that** the platform adapts to our organization's specific needs.

**Acceptance Criteria:**
- [ ] Plugin catalog (local or marketplace) lists available plugins
- [ ] Can install, enable, disable, and uninstall plugins
- [ ] Plugins declare required permissions (API scopes, data access)
- [ ] Plugin configuration UI is rendered from the plugin's schema
- [ ] Plugin updates are versioned; can roll back to previous version

### US-7.4 — View Audit Logs
**As** Sam,
**I want** to view an immutable audit log of all actions taken in the platform,
**So that** we have a complete record for security and compliance purposes.

**Acceptance Criteria:**
- [ ] Every create, update, delete, login, and permission change is logged
- [ ] Log entries include: timestamp, actor, action, entity, old/new values, IP address
- [ ] Can search and filter logs by date range, actor, action type, entity
- [ ] Logs cannot be modified or deleted (append-only)
- [ ] Can export logs to SIEM (Splunk, Elastic) via syslog or webhook

### US-7.5 — Manage Taxonomy & Configuration
**As** Sam,
**I want** to customize the platform's taxonomy (risk categories, rating scales, lifecycle states),
**So that** Ground Control uses our organization's language and processes.

**Acceptance Criteria:**
- [ ] Can add, edit, reorder, and retire taxonomy values (risk categories, control types, etc.)
- [ ] Can configure scoring scales (3-point, 5-point, custom) with labels and colors
- [ ] Can configure workflow states and transitions for each entity type
- [ ] Changes to taxonomy are versioned; historical data retains original labels
- [ ] Can import/export taxonomy configuration as YAML/JSON

---

## Epic 8: Agent-Specific Stories

### US-8.1 — Register an Agent
**As** Sam,
**I want** to register an AI agent with specific permissions and an API credential,
**So that** agents can interact with the platform under controlled, auditable access.

**Acceptance Criteria:**
- [ ] Can register an agent with: name, description, owner (human), allowed scopes
- [ ] Agent receives OAuth2 client credentials (client_id + client_secret)
- [ ] Agent is assigned a role that limits its permissions
- [ ] Agent registration is logged in the audit trail
- [ ] Can revoke agent credentials immediately

### US-8.2 — Agent Retrieves Assignments
**As** an AI Agent,
**I want** to query the API for test procedures assigned to me,
**So that** I know what work to perform.

**Acceptance Criteria:**
- [ ] `GET /api/v1/agents/{agent_id}/assignments` returns pending test procedures
- [ ] Response includes: control context, test steps, prior period results, linked evidence
- [ ] Can filter by assessment campaign, priority, due date
- [ ] Pagination and rate limiting protect the API

### US-8.3 — Agent Submits Results
**As** an AI Agent,
**I want** to submit structured test results for a test procedure,
**So that** my analysis is recorded and routed for human review.

**Acceptance Criteria:**
- [ ] `POST /api/v1/test-procedures/{id}/results` accepts structured result payload
- [ ] Payload includes: per-step results, overall conclusion, confidence score, evidence references, provenance metadata
- [ ] Results are flagged as `agent_produced = true`
- [ ] Submission triggers notification to the assigned human reviewer
- [ ] Invalid or incomplete payloads return descriptive 422 errors

### US-8.4 — Agent Suggests Control Mappings
**As** an AI Agent,
**I want** to analyze a control description and suggest framework mappings,
**So that** compliance analysts can quickly map new controls to relevant requirements.

**Acceptance Criteria:**
- [ ] `POST /api/v1/controls/{id}/suggest-mappings` triggers agent analysis
- [ ] Response includes: suggested framework requirements with confidence scores
- [ ] Suggestions are presented to the human analyst for approval/rejection
- [ ] Approved suggestions create the actual mappings
- [ ] Suggestion history is retained for agent performance tracking
