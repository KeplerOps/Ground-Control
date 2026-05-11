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

> **Amended by ADR-029 (2026-05-03; further amended 2026-05-09 per issue #804):** the human-touchpoint count drops from two to one (PR merge only). Plan approval is no longer a synchronous gate; the plan is posted as a comment on the GitHub issue thread and the workflow proceeds directly to TDD. Decisions on review findings are also recorded as issue comments. Phase B's traceability and status-transition requirements move to Phase D (after reviews). Codex review now runs as a single pre-push pass (Step 6.5) — the post-push codex review (former Step 12) is removed from the SKILL but remains as tool-layer defense-in-depth — and is hard-capped at three cycles. Every successful cycle posts a verbatim findings record to the resolved issue thread. The phase structure (A/B/C/D) and gate ordering below are otherwise preserved. Read ADR-029 for the full new gate model.
>
> **Amended by issue #801 (2026-05-05):** Phase A's TDD step gains a narrow documentation-only carve-out (no executable behavior in the diff + every clause/criterion protected by a named structural gate; declared in the plan and re-stated on the issue thread; substring/snapshot tests are not gates). The Phase B completion gate re-validates the carve-out against the *actual* diff with a two-check sweep over the union of committed (`<base-ref>...HEAD`), staged, unstaged, and untracked paths — both path (every changed path in the documentation set) and content (every diff hunk free of executable behavior) — because a path check alone can miss executable behavior carried in an otherwise-doc-named file, and an `<base-ref>...HEAD`-only check would miss uncommitted changes still in the working tree. Phase D's traceability reconciliation gains a backfill rule for runs whose diff finalizes a requirement whose structural implementation lives in pre-existing files shipped under a sibling requirement — IMPLEMENTS links are backfilled onto those artifacts of record, bounded by the requirement's concrete subject matter. See `skills/implement/SKILL.md` Step 4.4, Step 6, and Step 16 for the operative prose, and `architecture/notes/implement-docs-only-preexisting-traceability-guardrails.md` for the preflight design context.
>
> **Amended by issue #842 (2026-05-10):** Phase A's issue-resolution step (Step 1) now flags the resolved GitHub issue **in-progress** immediately after checking out the feature branch — it applies an `in-progress` label (created on demand if the repo lacks it, without overwriting an existing label's metadata — no `gh label create --force`) and posts a pickup comment on the thread recording the driver, the checked-out branch (`git branch --show-current`), and an ISO-8601 timestamp — so a maintainer scanning the issue list, or another agent, can see at a glance that the issue is in flight. This is operational visibility only: the pickup comment is not a phase marker, the plan comment, a findings record, or the final report, and it gates nothing. Phase D's issue-closure step (Step 18) removes the label; a run that escalates to the user without reaching Step 18 intentionally leaves it set (the issue *was* picked up and the work is paused, not finished). No new policy rule — the existing `workflow-guardrail-sync` rule already keeps `skills/implement/SKILL.md` in sync with the workflow docs and this ADR. See `skills/implement/SKILL.md` Step 1 and Step 18 for the operative prose, and `architecture/notes/in-progress-issue-flag-preflight.md` for the preflight design context.
>
> **Amended by issue #848 (2026-05-10):** Phase B's "CHANGELOG update" artifact is replaced by a **changelog fragment** convention. Per-PR release notes ship as files under `changelog.d/<issue>.<type>.md` (or `+<slug>.<type>.md` for issue-free entries), where `<type>` is one of the six Keep-a-Changelog categories (`security`, `added`, `changed`, `deprecated`, `removed`, `fixed`). CI-only and docs-only diffs may ship without a fragment; there is no "pure refactor" carve-out because the enforcement is path-based and cannot distinguish a behavior-preserving refactor from a feature change — refactors under application source still file a fragment. A direct `CHANGELOG.md` edit does NOT satisfy a source-changing diff (it would re-open the rebase-storm pathology this change exists to prevent); direct edits are reserved for release-collation commits whose diff is `CHANGELOG.md` plus the consumed fragments. Release-time `uvx towncrier build` collates fragments into `CHANGELOG.md` at the `<!-- towncrier release notes start -->` marker. The change exists so concurrent PRs cannot conflict on the same `CHANGELOG.md` line range — eliminating a structural rebase-storm pathology that costs CI capacity and engineering time with no behavioural benefit. Enforcement is repo-native: `tools/policy/checks.py::run_changelog_fragment_check` (codes `changelog-signal-missing`, `changelog-fragment-invalid-name`, `changelog-fragment-infrastructure`) covers the completion gate; `.claude/hooks/verify-implementation.sh` mirrors the same vocabulary as a host-local Stop hook, and the `hook-matches-policy-vocabulary` policy test keeps the two layers in sync. The convention itself is the template for other Ground-Control-aware repos — `towncrier.toml`, `changelog.d/_template.md.jinja`, `changelog.d/README.md`, the `CHANGELOG.md` marker, the `.gitattributes` `CHANGELOG.md merge=union` belt-and-suspenders rule, and the `.gc/plan-rules.md` fragment rule are copied/adapted, not generated. Issue-thread planning (ADR-029), Codex review routing (ADR-029 + #804), traceability semantics (Step 16), and release automation triggers (per-repo) are unchanged. See `skills/implement/SKILL.md` Step 4 / Step 6 / Step 4.4 / Step 15 / Step 16 for the operative prose, `changelog.d/README.md` for the convention, and `architecture/notes/changelog-fragments-preflight.md` for the preflight design context.

1. **Phase A — Plan and Implement**: Fetch requirement, create GitHub issue, explore codebase, produce plan ~~for human approval~~ *posted as an issue comment per ADR-029*, implement, verify clause-by-clause.
2. **Phase B — Quality Gate**: Hard completion gate requiring build success (`make check`), ~~CHANGELOG update~~ *(changelog fragment under `changelog.d/` required for any source-changing diff; CI-only and docs-only diffs need no signal; direct `CHANGELOG.md` edits are reserved for release-collation commits and do NOT satisfy a source-changing diff — per the #848 amendment)*, ~~traceability links (IMPLEMENTS + TESTS)~~ *(moved to Phase D per ADR-029)*, ~~requirement status ACTIVE~~ *(moved to Phase D per ADR-029)*, and clause-by-clause mapping.
3. **Phase C — Stage, Commit, Push**: Stage files, pre-commit retry loop (max 5), push to feature branch.
4. **Phase D — Multi-Reviewer Ship Pipeline**: Create PR, CI monitor, SonarCloud quality gate, ~~Codex cross-model review~~ *(post-push codex review removed by issue #804; the canonical codex pass is the pre-push Step 6.5, hard-capped at 3 cycles per ADR-029)*, ~~Claude `/review` and `/security-review` skills~~ *(test quality review only; SKILL Step 13)*, fix all findings, transition requirement to ACTIVE, reconcile traceability links, present for human merge.

**Human touchpoints**: ~~Exactly two — plan approval (Phase A) and PR merge (Phase D)~~ *Per ADR-029, exactly one — PR merge (Phase D). Plan, review findings, and decisions on findings are recorded as comments on the GitHub issue thread.*

**Zero-deferral policy**: All reviewer findings are fixed before the PR is presented. No findings are deferred to follow-up work.

> **Amended by ADR-029 (2026-05-10 per issue #830):** the zero-deferral policy is now mechanically enforced — a `PreToolUse` hook (`.claude/hooks/block-defer-language.py`) blocks `gh issue/pr {create,edit,comment,close}` calls carrying deferral-disposition language, and `bin/policy` flags the same language in the PR body at completion gate. Filing a tracking issue does not convert a deferral into a valid disposition; the only valid dispositions are `fix`, `wontfix` (with explicit user authorization), or `not-applicable` (with rationale). Codex review additionally classifies each finding `one-off` or `class`; a `class` finding must be fixed at the category level (one structural point of repair applied to every instance), not whack-a-mole'd to the reviewer-named site. See ADR-029 § "`defer` is not a valid disposition" for the full contract.

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
