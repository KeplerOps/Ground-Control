# Research Workflow — Capability Requirements

Status: Draft
Owner: Ground Control core
Companion ADRs: ADR-024, ADR-025, ADR-026
Companion skill: `/research` (`.claude/skills/research/SKILL.md`)

## 1. Purpose

Extend Ground Control with a software-mediated research workflow that is the
peer of `/implement`. Where `/implement` drives a requirement from idea to
merged code, `/research` drives a research question from idea to validated,
traceable findings.

The workflow must support — at minimum — the following research modes the
project already needs to handle:

- Literature reviews, including the case where a usable lit review already
  exists and should be consumed rather than re-done.
- Software-mediated experiments, including adversarial / purple-team labs
  (e.g. <https://github.com/Brad-Edwards/aptl>-style ranges) where the agent
  needs to plan, run, and analyze attacks and detections against asset
  topology under explicit authorization.
- Methodology co-design with the agent: the human and the agent iterate on
  what the right approach is *before* spending effort on data collection.
- Mixed-mode research that combines several of the above.

## 2. Non-goals

- No new persistence: the workflow re-uses Requirements, Documents, ADRs,
  Assets, Observations, TraceabilityLinks, and VerificationResults. New
  entity types are out of scope until a future requirement proves the
  existing ones insufficient (per ADR-J002 / `architecture-model-artifacts`).
- No bespoke citation manager, statistics package, or LIMS. The workflow
  *orchestrates* tools the operator already runs; it does not re-implement
  them.
- No automatic execution of offensive tooling against unauthorized targets.
  Authorization is a hard precondition surfaced as a gate, not a paperwork
  field.

## 3. Personas

| Persona | What they need from `/research` |
|---|---|
| **Lit reviewer.** Wants a defensible, reproducible literature survey. | Can produce a charter, run a lit review (or import an existing one), and ship a findings doc with traceability to source artifacts. |
| **Experimentalist.** Has a hypothesis and a lab. | Can co-design a protocol, capture observations and verification results, and link results back to the hypothesis. |
| **Purple-team researcher.** Drives offensive/defensive experiments in a lab range. | Gets a hard authorization/safety gate, asset-topology-aware planning, and per-asset evidence capture before any execution phase runs. |
| **Reviewer.** Reads someone else's research output. | Sees the charter, methodology, observations, and synthesis as one connected graph traversal — same affordance as `/implement` provides for code. |

## 4. Functional Requirements

Each requirement is phrased so it can be filed in Ground Control as a real
requirement under a `research-workflow` wave. UIDs in this document are
illustrative; canonical UIDs are assigned when filed.

### 4.1 Phases

**RW-F001 — Composable phases.** The workflow shall be expressible as the
following ordered phases, any subset of which may be executed for a given
invocation:

1. **Charter** — declare the research question, hypothesis, scope, success
   criteria, threat-to-validity list, and authorization basis.
2. **Lit review** — produce or import a literature review document.
3. **Methodology co-design** — agree on approach, instruments, sampling /
   target selection, and analysis plan.
4. **Protocol** — write the executable, step-by-step procedure including
   pre-conditions, stopping conditions, and rollback.
5. **Safety / authorization preflight** — required when the protocol touches
   adversarial techniques, third-party systems, human subjects, or
   regulated data.
6. **Execution** — run the protocol, capturing artifacts and observations.
7. **Analysis** — apply the analysis plan to captured data.
8. **Synthesis** — write up findings linked to the charter's success
   criteria.
9. **Peer review** — cross-model review of methodology and synthesis.
10. **Publication / handoff** — close the loop by linking findings back to
    the charter, to any requirements or risks they affect, and to follow-up
    research questions.

**RW-F002 — Phase selection.** The orchestrating skill shall accept a
`--from` and `--to` (or equivalent argument shape) so the operator can run a
contiguous slice of phases, e.g. "lit-review through protocol".

**RW-F003 — Phase skipping with provenance.** Each phase shall be skippable
when its output already exists. When skipped, the workflow shall record
which existing artifact (Document / ADR / TraceabilityLink) supplies that
phase's deliverable. A skipped phase without a referenced artifact is an
error, not a no-op.

**RW-F004 — Phase resumability.** A research question shall be resumable
mid-workflow without losing state. State lives in the graph — phase
deliverables are real artifacts (Documents, ADRs, Observations,
VerificationResults), not transient skill memory.

### 4.2 Charter

**RW-F010 — Charter authoring.** The Charter phase shall co-design (with
the agent) a charter document containing at minimum:

- Research question (one sentence).
- Hypothesis (or null hypothesis if exploratory).
- Scope and out-of-scope list.
- Success criteria (testable).
- Threats to validity that the operator already anticipates.
- Authorization basis: explicit statement of who authorized the research,
  what assets / data / subjects are in scope, and what is excluded.
- Mode selector: `LITERATURE`, `EXPERIMENTAL`, `ADVERSARIAL_LAB`,
  `MIXED`. The selector drives later phase defaults (e.g.
  `ADVERSARIAL_LAB` enables RW-F050 by default).

**RW-F011 — Charter as Requirement.** The charter shall be filed as a
Requirement in the active Ground Control project, using a UID convention
that distinguishes research questions from software requirements
(e.g. `<project>-RQ###`). Hypotheses tested by that question are recorded
as child requirements via `REFINES`.

**RW-F012 — Charter co-design escape hatch.** The agent shall stop and ask
the human to fill gaps when any of the minimum charter fields cannot be
derived from the conversation or prior artifacts. The agent shall not
fabricate authorization, success criteria, or threats to validity.

### 4.3 Literature review

**RW-F020 — Lit review artifact.** A lit review shall produce (or reference)
a Document in Ground Control with: question(s) addressed, search strategy,
inclusion/exclusion criteria, a structured summary of the surveyed sources,
and a synthesis section.

**RW-F021 — Use existing review.** When the operator supplies an existing
lit review (path or URL), the workflow shall import / link it as the
phase's deliverable rather than re-running the survey.

**RW-F022 — Source provenance.** Each surveyed source shall be linkable as
a TraceabilityLink with `artifact_type=DOCUMENTATION` (or `SPEC` where
appropriate) so coverage queries can find it later.

**RW-F023 — Gap identification.** The lit review phase shall produce an
explicit list of gaps that motivates the rest of the research. If no gaps
are identified the workflow shall surface that and ask the human whether
to terminate.

### 4.4 Methodology co-design

**RW-F030 — Approach co-design.** The methodology phase shall produce or
update an ADR documenting the chosen approach and the alternatives
considered, with the same ADR lifecycle as the rest of the project.

**RW-F031 — Reuse over invention.** The Codex methodology preflight prompt
shall instruct the agent to reuse existing instruments, ranges, datasets,
and analysis tooling already present in the repo or referenced from prior
research before proposing new ones.

**RW-F032 — Reproducibility constraints.** The methodology shall include
sufficient detail (versions, seeds, configurations, target inventories) to
let an independent operator reproduce the experiment.

### 4.5 Protocol

**RW-F040 — Protocol artifact.** The Protocol phase shall produce a
Document or `ArtifactType.SPEC`-linked file containing: pre-conditions,
required assets and tools, step-by-step procedure, success/abort criteria,
data capture plan, and rollback plan.

**RW-F041 — Asset binding.** When the protocol references operational
assets (lab hosts, networks, data stores) those references shall be
recorded as TraceabilityLinks to the corresponding `Asset` records so the
existing topology / impact analysis tools work for research scopes too.

### 4.6 Safety / authorization preflight

**RW-F050 — Mandatory for adversarial mode.** When the charter mode is
`ADVERSARIAL_LAB` (or when the protocol references techniques flagged as
high-blast-radius), the workflow shall require a Safety Preflight phase
before Execution.

**RW-F051 — Safety preflight content.** The preflight shall produce a
checklist artifact covering at minimum: in-scope asset list, out-of-scope
list, blast-radius bound, network/identity isolation guarantees, data
handling rules, abort conditions, and named authorizing party.

**RW-F052 — Hard gate.** Execution phase tooling shall refuse to run when
a required Safety Preflight artifact is missing or unresolved (status not
`ACCEPTED` for the equivalent ADR / not signed-off for a checklist
document).

**RW-F053 — Authorization is not paperwork.** The preflight prompt shall
reject vague authorization ("approved by management") and require named
roles, named assets, and a dated scope statement.

### 4.7 Execution

**RW-F060 — Observations as data.** Execution shall capture results as
Observations on the relevant Assets (using existing
`OBSERVATION_CATEGORIES`) and / or as VerificationResults when the protocol
step is a defined check.

**RW-F061 — Artifact capture.** Raw artifacts (logs, packet captures,
notebooks, screenshots) shall be either stored in-repo with stable
relative paths and linked via TraceabilityLink, or referenced via
`AssetExternalId` when held in an external store. The graph shall always
know where the artifact lives.

**RW-F062 — Step-level traceability.** Each protocol step that produces
data shall create a TraceabilityLink from the parent research question /
hypothesis to the captured artifact, with `LinkType.VERIFIES` when the
step is a defined check and `LinkType.DOCUMENTS` otherwise.

**RW-F063 — Idempotent re-runs.** Re-running a protocol shall not destroy
prior runs' data. Each run is a new set of Observations /
VerificationResults; old ones remain queryable via the audit timeline.

### 4.8 Analysis & synthesis

**RW-F070 — Analysis plan adherence.** The Analysis phase shall apply the
analysis plan recorded in the methodology ADR. Deviations are allowed but
shall be recorded as updates to that ADR (or a superseding ADR) before the
synthesis is written.

**RW-F071 — Synthesis artifact.** Synthesis shall produce a Document
linked back to the charter via `LinkType.DOCUMENTS`, with a section per
success criterion stating "met / not met / inconclusive" and citing the
specific Observations / VerificationResults that justify the call.

**RW-F072 — Negative results are first-class.** A failed hypothesis is a
valid synthesis outcome. The workflow shall not push the operator to
manufacture a positive finding.

### 4.9 Peer review

**RW-F080 — Cross-model methodology review.** Before Publication, a Codex
research-review pass shall examine the charter, methodology ADR, protocol,
and synthesis for: methodology soundness, threats to validity that were
missed, statistical or analytical errors, scope creep, and unsupported
claims. The review prompt shall be exhaustive (no triage), matching the
existing `gc_codex_review` philosophy.

**RW-F081 — Review fix loop.** All findings from the cross-model review
shall be fixed before the synthesis is presented for human review.

### 4.10 Publication / handoff

**RW-F090 — Closing the loop.** Publication shall: transition the research
question Requirement to `ACTIVE`, ensure `IMPLEMENTS` / `TESTS` / `DOCUMENTS`
links exist as appropriate, and create explicit follow-up Requirements
for any open questions identified in synthesis (`RELATED` or `REFINES`).

**RW-F091 — Effect on the rest of the graph.** Where research findings
affect existing requirements, ADRs, risks, or controls, Publication shall
create or update the corresponding traceability links so the impact is
visible from the affected node.

## 5. Non-functional Requirements

**RW-N001 — Reuse first.** The workflow shall not introduce new entity
types or new persistence layers in the backend. It is a skill +
prompt-builder + helper layer over existing primitives. Any deviation
requires a new ADR explicitly justifying the new entity.

**RW-N002 — Auditability.** Every phase deliverable shall be a real,
audited artifact in Ground Control (Requirement, ADR, Document,
TraceabilityLink, Observation, VerificationResult). No phase output
exists only inside the agent's working memory.

**RW-N003 — Reproducibility.** Given the artifacts the workflow produces
plus the repo at the same commit, an independent operator shall be able
to re-run the protocol. The methodology phase is responsible for ensuring
this (RW-F032).

**RW-N004 — TDD-friendly helpers.** The new MCP helpers (prompt builders,
arg builders, parsers) shall be pure functions tested in the existing
`lib.test.js` style. Anything that shells out to `codex`, `gh`, or the
filesystem shall delegate to the helpers under test.

**RW-N005 — No silent gate weakening.** Like ADR-021, the gate structure
(safety preflight, peer review, publication) shall be specified in this
document and the matching ADRs, not only in the skill markdown.

**RW-N006 — Operator-supplied paths only.** Any helper that ingests a file
(e.g. an existing lit review) shall validate that the path is absolute and
operator-supplied, matching the existing pattern in
`readOperatorSuppliedFile`.

**RW-N007 — Project-scoped.** Every research artifact created by the
workflow shall be scoped to the repo's Ground Control project (resolved
via `gc_get_repo_ground_control_context`), matching the rest of the
agentic loop.

## 6. Workflow modes

The composability requirement (RW-F001 / RW-F002) is exercised by these
named modes. The skill shall expose at least these modes and any subset
thereof:

| Mode | Phases run |
|---|---|
| `lit-review-only` | Charter → Lit review → Synthesis (lit-review-shaped) → Peer review → Publication |
| `protocol-only` | Charter → Methodology → Protocol → Peer review (no execution) |
| `co-design` | Charter → Lit review (optional) → Methodology → Peer review |
| `experiment` | Charter → Methodology → Protocol → Safety preflight (if needed) → Execution → Analysis → Synthesis → Peer review → Publication |
| `purple-lab` | `experiment` with `ADVERSARIAL_LAB` mode and Safety preflight required |
| `full` | All ten phases |

## 7. Out-of-band concerns deferred to other ADRs

- Numerical / statistical analysis tooling integration: out of scope; the
  workflow records *which* tool was run, not how to run it.
- Publication beyond the repo (papers, blog posts, briefings): out of
  scope; Publication phase ends at the graph and the repo. External
  publishing is a separate human action.
- Cost / budget tracking: out of scope.

## 8. Acceptance criteria for this capability extension

The first delivery of this capability is accepted when:

1. The `/research` skill exists and runs the `co-design` and
   `lit-review-only` modes end-to-end against the existing MCP surface,
   creating real Documents, ADRs, and TraceabilityLinks.
2. The Codex preflight / review prompts for the new phases are pure
   functions exported from `lib.js`, tested in `lib.test.js`.
3. ADR-024, ADR-025, and ADR-026 are accepted and indexed in the ADR
   README.
4. `make rapid` and the MCP `node --test lib.test.js` test suite pass.
5. The Research Workflow capability is itself filed as a parent
   Requirement in the Ground Control project, with the ten phase
   requirements as children.
