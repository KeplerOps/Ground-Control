# ADR-024: Research Workflow as Composable Phases on Existing Primitives

## Status

Accepted

## Date

2026-05-05

## Context

Ground Control already has a gated agentic development loop (`/implement`,
ADR-021) for driving software requirements from idea to merged code. The
project also needs to drive software-mediated *research* — literature
reviews, experiments, adversarial / purple-team lab runs — with the same
level of traceability, auditability, and review rigor.

The research domain has structurally different inputs and outputs from the
software-engineering domain:

- The unit of work is a research question or hypothesis, not a feature.
- The deliverable is evidence and synthesis, not merged code.
- Literature is a first-class artifact and a frequent input.
- Experiments operate on lab assets and produce observations and
  verification outcomes.
- Adversarial-mode research has a hard authorization / safety pre-condition
  that does not appear in the software loop.

Two implementation paths exist:

1. **Bespoke research subsystem.** Add new entity types (research
   question, hypothesis, experiment run, finding) with their own JPA
   entities, REST endpoints, MCP tools, frontend pages, and tests. Mirrors
   the size of the existing requirements / risk / control subsystems.
2. **Composable workflow over existing primitives.** Treat research as
   a workflow layer (skill + MCP helpers) that re-uses Requirements,
   ADRs, Documents, Assets, Observations, VerificationResults, and
   TraceabilityLinks. Mirrors the architecture of the `architecture-model
   artifacts` design note (do not introduce parallel entity models when
   the existing primitives fit).

## Decision

Adopt path 2: implement the research workflow as **composable phases
layered on existing primitives**. Concretely:

1. The workflow is exposed as a single `/research` skill (see
   `.claude/skills/research/SKILL.md`) with selectable phases. Phase
   selection plus the charter `mode` (`LITERATURE`, `EXPERIMENTAL`,
   `ADVERSARIAL_LAB`, `MIXED`) determines which phases run.
2. The phases are:
   1. Charter
   2. Lit review (skippable when an existing review is supplied)
   3. Methodology co-design
   4. Protocol
   5. Safety / authorization preflight (mandatory for `ADVERSARIAL_LAB`,
      see ADR-026)
   6. Execution
   7. Analysis
   8. Synthesis
   9. Peer review (cross-model, exhaustive, no triage)
   10. Publication / handoff
3. Each phase deliverable is a real Ground Control artifact:
   - Charter, hypotheses, follow-up questions → Requirements
     (UID convention from ADR-025).
   - Methodology decisions → ADRs.
   - Lit review, protocol, synthesis → Documents (with sections /
     content) plus optional `ArtifactType.SPEC` /
     `ArtifactType.DOCUMENTATION` traceability links.
   - Lab assets and topology → Assets, Asset relations, Asset links.
   - Captured experiment data → Observations and / or
     VerificationResults plus TraceabilityLinks.
4. The MCP layer adds **only** pure helper functions in
   `mcp/ground-control/lib.js`:
   - Prompt builders (`buildResearchCharterPrompt`,
     `buildResearchLitReviewPrompt`,
     `buildResearchMethodologyPreflightPrompt`,
     `buildResearchSafetyPreflightPrompt`,
     `buildResearchSynthesisReviewPrompt`).
   - Arg builders (`buildResearchExecArgs`).
   - State helpers (`buildResearchPlan`, `parseResearchCharter`).
   - Constants (`RESEARCH_PHASES`, `RESEARCH_MODES`,
     `RESEARCH_PHASE_REQUIREMENTS`).
   These are testable in `lib.test.js` in the same style as the existing
   Codex helpers (ADR-021).
5. No new backend entities, no new database migrations, no new graph
   node types are introduced for the first delivery. If a future
   research need genuinely cannot be expressed via the existing
   primitives, that is a new ADR-worthy decision.
6. The gate structure (safety preflight, peer review, publication
   linkage) is captured in the requirements doc
   (`docs/research/RESEARCH_WORKFLOW_REQUIREMENTS.md`) and in this ADR,
   so the gates cannot be silently weakened by editing only the skill
   markdown (mirrors ADR-021 RW-N005).

The workflow:

- **Depends on** the Requirements, ADR, Document, Asset, Observation,
  VerificationResult, and TraceabilityLink subsystems.
- **Refines** the gated agentic loop (ADR-021) by adding a research
  variant of the same shape.
- **Related to** ADR-025 (research question / hypothesis modeling) and
  ADR-026 (safety / authorization gate).

## Consequences

### Positive

- No new persistence, schema, or MCP entity surface to maintain. All
  research artifacts live in the same audit trail and graph as
  requirements and code.
- Existing analysis tools (cycle detection, coverage gaps, impact
  analysis, asset topology, baselines, timeline export) work for
  research scopes the moment a research question is filed.
- The workflow can ship in days and iterate, instead of waiting for a
  parallel research subsystem.
- The pattern is consistent with the
  `architecture-model-artifacts` design note: prefer existing
  primitives over new ones.

### Negative

- Research-specific semantics are encoded as conventions
  (UID prefixes, traceability link types, ADR titles) rather than as
  schema. Conventions are easier to break than constraints. ADR-025
  formalizes the conventions to mitigate this.
- A research question Requirement looks the same to the type system
  as a software Requirement. Filtering UI / queries that need to
  distinguish them must inspect the UID or a dedicated metadata
  field.

### Risks

- **Convention drift.** If two operators independently invent UID
  schemes for research questions, the graph fragments. Mitigation:
  ADR-025 specifies the convention and the `/research` skill is the
  only sanctioned authoring path.
- **Premature reuse.** Forcing a future research need to fit existing
  primitives may produce ugly conventions. The decision to add a new
  entity must be a deliberate ADR, not a drift in the helper layer.
