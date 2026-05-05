---
name: research
description: End-to-end research workflow — charter, lit review, methodology, protocol, safety gate, execution, analysis, synthesis, peer review, publication
argument-hint: <research-question-uid> [--mode <MODE>] [--from <PHASE>] [--to <PHASE>] [--skip <PHASE,PHASE>] [--existing-lit-review <abs-path>]
disable-model-invocation: true
---

# Research Workflow: $ARGUMENTS

This skill is the research peer of `/implement` (ADR-021, ADR-024). It drives a
research question or hypothesis from charter through publication, capturing
every deliverable as a real audited artifact in Ground Control's graph.

The workflow is composable: the operator can run only a slice of phases and
skip phases whose deliverables already exist. Each skipped phase must reference
an existing artifact (RW-F003) — silent skipping is an error.

The argument is the full research question UID per ADR-025
(`<PROJECT>-RQ###` or `<PROJECT>-H###`).

---

## Step 0: Validate the UID and the repo context

1. Run `pwd` to capture the absolute repository root.

2. Call `gc_get_repo_ground_control_context` with `repo_path` set to that
   path. If the result is not `status: "ok"`, stop and ask the operator to
   add the Ground Control context to `AGENTS.md`. Do not guess the project.

3. Use the `project` from that result for every subsequent Ground Control
   call.

4. Call `gc_research_validate_uid` with the operator-supplied UID. If
   `ok` is `false`, stop and report the errors. Do not invent a UID.

5. Parse any of these flags from `$ARGUMENTS`. Sensible defaults are listed.

   | Flag | Default | Meaning |
   |---|---|---|
   | `--mode <LITERATURE\|EXPERIMENTAL\|ADVERSARIAL_LAB\|MIXED>` | `EXPERIMENTAL` | Charter mode (ADR-026 gate trigger) |
   | `--from <PHASE>` | `CHARTER` | First phase to run, inclusive |
   | `--to <PHASE>` | `PUBLICATION` | Last phase to run, inclusive |
   | `--skip <PHASE,PHASE,...>` | empty | Phases to skip; each must reference an existing artifact |
   | `--existing-lit-review <abs-path>` | none | Skip authoring a new lit review; ingest the supplied document |
   | `--require-safety-preflight` | off (defaults via mode) | Force the gate even for non-adversarial modes |

6. Call `gc_research_select_phases` with the parsed flags. If the result has
   non-empty `errors`, stop and report them. Otherwise treat the returned
   `phases` array as the ordered plan.

7. Echo the plan back to the operator and request approval before
   continuing. This is the human checkpoint that mirrors `/implement`'s
   plan approval.

---

## Step 1: Fetch (or create) the research question

1. Call `gc_get_requirement` with the UID and the project. If the
   requirement does not exist, the Charter phase is responsible for
   creating it; otherwise note its UUID, status, statement, wave.

2. Call `gc_get_traceability` with the UUID and note any existing
   artifacts. These are the candidate references for any `--skip` phases.

---

## Phase: CHARTER

Goal: produce or update the charter Requirement plus its rationale, scope,
authorization basis, and threats-to-validity list.

1. Call `gc_codex_research_phase` with:
   - `phase: "CHARTER"`
   - `requirement_uid: <uid>`
   - `repo_path: <abs path>`
   - `project: <gc project>`
   - `mode: <selected mode>`
   - `prior_context: <conversation context>` (optional)

2. Read Codex's output and the changed files. If Codex surfaced gaps,
   relay them to the operator and stop until they are resolved.

3. If the research question Requirement does not yet exist, file it via
   `gc_create_requirement` using the charter content. Use the UID exactly
   as provided.

4. For each hypothesis the charter introduces, file a child Requirement
   with the matching `H###` UID and a `REFINES` relation to the parent.

5. Create traceability links (`gc_create_traceability_link`) from the
   research question to the charter document and to any explicit
   authorization-basis artifact.

If `CHARTER` is in `--skip`, reference the existing charter Document and
proceed.

---

## Phase: LIT_REVIEW

Goal: produce a lit review Document or import an existing one.

1. If `--existing-lit-review <path>` was provided:
   - Call `gc_codex_research_phase` with `phase: "LIT_REVIEW"`,
     `existing_lit_review_path: <abs path>`. Codex will ingest, summarize,
     and link.
2. Otherwise call the same tool without the path. Codex will author a new
   lit review Document.

3. Either way, ensure a TraceabilityLink with `artifact_type=DOCUMENTATION`
   and `link_type=DOCUMENTS` exists from the research question to the
   resulting Document.

4. Create individual TraceabilityLinks for cited sources where each is
   identifiable.

If `LIT_REVIEW` is in `--skip`, reference the supplied existing lit
review's TraceabilityLink and proceed.

---

## Phase: METHODOLOGY

Goal: lock the chosen approach into an ADR.

1. Call `gc_codex_research_phase` with:
   - `phase: "METHODOLOGY"`
   - `charter_doc: <charter text>`
   - `lit_review_summary: <lit review summary>`
   - `mode: <selected mode>`

2. Codex will produce or update an ADR. Once authored, transition the ADR
   to `ACCEPTED` only after the operator approves.

3. Create a TraceabilityLink with `artifact_type=ADR` and
   `link_type=DOCUMENTS` from the research question to the ADR.

If `METHODOLOGY` is in `--skip`, reference the existing methodology ADR.

---

## Phase: PROTOCOL

Goal: write the executable, step-by-step procedure.

The Protocol phase is operator-driven (the agent assists; Codex is not
auto-invoked). Produce a Document containing:

- Pre-conditions (assets, tools, data).
- Step-by-step procedure with stop / abort criteria.
- Data capture plan (what to record, where, how).
- Rollback plan.

For every Asset referenced (lab hosts, networks, identities, data stores),
create an AssetLink with `target_type=REQUIREMENT` to the research
question.

If `PROTOCOL` is in `--skip`, reference the existing protocol Document.

---

## Phase: SAFETY_PREFLIGHT (HARD GATE)

This phase is mandatory when `gc_research_requires_safety_preflight`
returns `required: true`. The Execution phase will refuse to run without
a complete, signed-off preflight artifact.

1. Call `gc_research_requires_safety_preflight` with the mode, the
   technique tags the protocol references, and any operator opt-in.

2. If `required: true`:
   1. Call `gc_codex_research_phase` with `phase: "SAFETY_PREFLIGHT"` and
      the protocol text. Codex will produce a checklist artifact draft.
      Codex must NOT pre-fill the sign-off block.
   2. Surface the draft to the operator. The operator names the
      authorizing party, completes the section bodies, and adds the
      `Signed-off-by:` line dated within the scope window.
   3. Once the operator persists the signed-off artifact at an absolute
      path, call `gc_research_parse_safety_preflight_checklist` with that
      path. If `ok: false`, fix the gaps and re-run; do NOT proceed to
      Execution. If `ok: true`, create a TraceabilityLink with
      `artifact_type=POLICY` (or `DOCUMENTATION` if persisted as a
      Document) and `link_type=DOCUMENTS` from the research question.

3. If `required: false`, skip this phase. Record the skip in the run log.

The skill MUST refuse to invoke the Execution phase tooling when this
gate is required and the parsed checklist is not `ok: true`.

---

## Phase: EXECUTION

Goal: run the protocol, capturing every result in the graph.

This phase is operator-driven. The agent assists by:

1. Refusing to start if Safety Preflight is required and not satisfied.
2. Recording each protocol step as it executes:
   - Defined checks → `gc_create_verification_result` with the
     appropriate `assurance_level` and `evidence_ref`.
   - Free-form observations → `gc_create_observation` on the relevant
     Asset, using existing `OBSERVATION_CATEGORIES`.
3. Capturing raw artifacts (logs, packet captures, notebooks) in-repo at
   stable relative paths and creating TraceabilityLinks
   (`artifact_type=DOCUMENTATION` or `SPEC`, `link_type=DOCUMENTS` or
   `VERIFIES`).
4. For artifacts held outside the repo (large lab captures, S3, etc.),
   record the location via `AssetExternalId`.
5. Re-runs append new Observations / VerificationResults; do NOT delete
   prior runs.

If `EXECUTION` is in `--skip`, reference the existing run's artifacts and
proceed.

---

## Phase: ANALYSIS

Goal: apply the analysis plan recorded in the methodology ADR.

This phase is operator-driven. The agent:

1. Re-reads the methodology ADR's analysis plan.
2. Produces an analysis artifact (Document or notebook checked into the
   repo) and links it from the research question.
3. If the analysis deviates from the plan, update the methodology ADR
   (or supersede it with a new ADR) before writing synthesis. Record the
   deviation.

If `ANALYSIS` is in `--skip`, reference the existing analysis artifact.

---

## Phase: SYNTHESIS

Goal: produce a findings Document linked back to the charter's success
criteria.

The agent (with operator) writes a synthesis Document containing:

- One section per success criterion: "met / not met / inconclusive",
  citing specific Observations or VerificationResults that justify the
  call.
- Threats to validity that materialized.
- Negative / null results stated plainly.

Create a TraceabilityLink with `artifact_type=DOCUMENTATION`,
`link_type=DOCUMENTS` from the research question to the synthesis
document.

If `SYNTHESIS` is in `--skip`, reference the existing synthesis Document.

---

## Phase: PEER_REVIEW

Goal: cross-model exhaustive review of methodology and synthesis. No
triage, no deferral — same rules as `/implement`'s code review.

1. Call `gc_codex_research_phase` with:
   - `phase: "PEER_REVIEW"`
   - `charter_summary: <text>`
   - `methodology_adr_uid: <uid>`
   - `synthesis_summary: <text>`

2. Read every finding and fix all of them. Update the synthesis,
   methodology ADR, or supplementary artifacts as required.

3. Re-run `PEER_REVIEW` after fixing if the fix was substantial enough
   that an independent re-review is warranted.

If `PEER_REVIEW` is in `--skip`, the workflow refuses to proceed to
Publication. Peer review is not optional.

---

## Phase: PUBLICATION

Goal: close the loop on the graph.

1. Transition the research question Requirement to `ACTIVE` if it is not
   already.

2. Confirm every required TraceabilityLink exists by re-fetching with
   `gc_get_traceability`:
   - Charter Document → `DOCUMENTS`
   - Lit review Document → `DOCUMENTS`
   - Methodology ADR → `DOCUMENTS`
   - Protocol Document → `DOCUMENTS`
   - Safety Preflight (when applicable) → `DOCUMENTS`
   - Execution artifacts → `DOCUMENTS` / `VERIFIES`
   - Analysis Document → `DOCUMENTS`
   - Synthesis Document → `DOCUMENTS`

3. For every follow-up question the synthesis surfaced, file a new
   research question Requirement with a `RELATED` relation back to the
   originating question.

4. Where findings affect existing requirements, ADRs, risks, or
   controls, create or update the corresponding traceability links so
   the impact is visible from the affected node.

5. If `--skip` was used for any phase, confirm the referenced existing
   artifacts are still linked.

---

## Final Report

Produce a concise summary covering:

- Research question UID and final status.
- Mode and which phases ran (vs. skipped, with provenance).
- Charter, methodology ADR, lit review, protocol, safety preflight (if
  any), synthesis Document UIDs and paths.
- Codex peer-review findings and how each was addressed.
- Follow-up research questions filed.
- Confirmation that the gate (when required) was satisfied prior to
  Execution.

Do NOT merge any PR or publish anywhere outside the graph. Publication
ends at Ground Control's audit trail; any external publication is a
separate human action.
