# ADR-021: Gated Agentic Development Loop

## Status

Accepted

## Date

2026-04-05

## Context

Ground Control is built by AI agents using the `/implement` skill, which orchestrates the full lifecycle from requirement to merged PR. The skill has evolved to include multiple quality gates, automated reviewers, and human checkpoints, but the gate structure is defined only in the skill's markdown instructions — it is not captured as a requirement, not traceable, and not self-documenting within Ground Control's own graph.

If the skill is modified, gates can be weakened or removed without any formal record of what was lost. Additionally, the existing GC-O series requirements (GC-O001 Self-Managed Requirements, GC-O004 Agent Workflow Traceability Standards, GC-O005 Requirement-Before-Code Policy) establish individual constraints but do not specify how those constraints compose into a coherent end-to-end workflow with enforced sequencing.

## Decision

Codify the gated agentic development loop as a first-class requirement (GC-O007) with four mandatory phases:

1. **Phase A — Plan and Implement**: Fetch requirement, create GitHub issue, explore codebase, produce plan for human approval, implement, verify clause-by-clause.
2. **Phase B — Quality Gate**: Hard completion gate requiring build success (`make check`), CHANGELOG update, traceability links (IMPLEMENTS + TESTS), requirement status ACTIVE, and clause-by-clause mapping.
3. **Phase C — Stage, Commit, Push**: Stage files, pre-commit retry loop (max 5), push to feature branch.
4. **Phase D — Multi-Reviewer Ship Pipeline**: Create PR, CI monitor, SonarCloud quality gate, Codex cross-model review, Claude `/review` and `/security-review` skills, fix all findings, present for human merge.

**Human touchpoints**: Exactly two — plan approval (Phase A) and PR merge (Phase D).

**Zero-deferral policy**: All reviewer findings are fixed before the PR is presented. No findings are deferred to follow-up work.

The workflow is implemented by:
- `/implement` skill (`.claude/skills/implement/SKILL.md`)
- Completion verifier agent (`.claude/agents/completion-verifier.md`)
- Repo-native policy guardrails (`architecture/policies/adr-policy.json`, `bin/policy`, `make policy`)
- Development workflow docs (`docs/DEVELOPMENT_WORKFLOW.md`, `docs/WORKFLOW.md`)
- Repo-local workflow config at `.ground-control.yaml` (with larger rule files under `.gc/`), resolved by `gc_get_repo_ground_control_context`. `AGENTS.md` carries a brief pointer to this config rather than the full workflow definition inline.

The requirement:
- **Depends on** GC-O004 (agent traceability standards) and GC-O005 (requirement-before-code policy) — these supply the constraints the quality gate enforces.
- **Related to** GC-O006 (SDD workflow tracking) and GC-C010 (configurable quality gates) — future automation surfaces.
- **Refines** GC-O001 (self-managed requirements) — specifies how the development process itself is governed.

## Consequences

### Positive

- Gate structure is traceable and protected from silent regression
- Self-referential traceability links dogfood GC's own traceability system
- Changes to `/implement` can be evaluated against the requirement to detect gate weakening
- Foundation for automating gate evaluation via GC-C010 quality gates
- Workflow conformance no longer depends only on Claude-specific user hooks; the repo itself now enforces the critical guardrails in pre-commit and CI

### Negative

- Requirement must be kept in sync with the `/implement` skill — creates a maintenance obligation

### Risks

- Over-specification could make the skill rigid; the requirement intentionally specifies gate structure (what must be checked) rather than implementation details (how to check it), preserving flexibility in tooling choices
