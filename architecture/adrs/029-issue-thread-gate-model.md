# ADR-029: Issue-Thread Gate Model

## Status

Accepted

## Date

2026-05-03

## Context

ADR-021 ("Gated Agentic Development Loop") and the prior GC-O007 statement
required two human touchpoints per `/implement` run: **plan approval** and
**PR merge**. Plan approval was implemented as `EnterPlanMode` followed by
explicit user sign-off before TDD began.

In practice, plan approval became ceremony. Empirically, more than 95% of
plans were accepted as-is. The synchronous gate added coordination tax (the
human had to be available at the moment the plan was ready) without
materially affecting outcomes — divergences from the plan were caught by
review steps later in the loop, not by the plan-approval gate.

Pulsar already operated under a non-conformant model in which the plan was
posted as a GitHub issue comment and the workflow proceeded directly to TDD.
ADR-027 ("Agent-Neutral Implement Workflow Packaging") initially treated
this as a transport choice — `plan.approval_gate=issue-comment` — but the
ADR-027 author flagged that proceeding without an approval signal would
violate GC-O007's two-touchpoint contract. That conflict prompted this ADR.

## Decision

GC-O007's contract is amended to **one human touchpoint: PR merge**. The
GitHub issue thread becomes the durable record of plan, review findings,
and decisions on findings.

### Touchpoints

- **PR merge** is the only synchronous human gate. The user reviews the issue
  thread (plan + findings + decisions) and the PR diff, then merges.
- **No plan-approval gate.** The `/implement` skill posts the plan to the
  GitHub issue as a comment via `gh issue comment` and proceeds directly to
  TDD. No `EnterPlanMode` call. No synchronous user-approval wait.

### Issue thread as durable record

Every artifact that previously implied a human gate is now recorded as a
comment on the GitHub issue:

- **Plan** — posted as a comment when `/implement` enters Phase A. Includes
  context, approach, files-to-change, verification steps, risks.
- **Review findings** — every finding from codex review, refactor review,
  test-quality review, and SonarCloud is posted to its native location (PR
  review comment for codex; issue comment summary for review aggregates).
  The issue thread carries a summary linking back to the PR comments.
- **Decisions on findings** — for every finding, the agent records its
  disposition as an issue comment: **fix**, **wontfix**, or
  **not-applicable**, with a one-line rationale. `defer` is not a valid
  decision: the workflow's contract is "fix every finding before PR is
  ready." Recording the disposition is mandatory; agent silence on a
  finding is treated as a process violation.
- **Status transitions and traceability reconciliation** — happen
  asynchronously in Phase D (after reviews), not in a synchronous gate.

### Replaces / amends

- **ADR-021** is **amended**, not superseded. Its phase structure (A/B/C/D)
  and gate ordering are preserved. Only the human-touchpoint count changes
  from 2 → 1, and plan publishing moves from `EnterPlanMode` → `gh issue
  comment`.
- **ADR-027** drops its "transport not bypass" clause; plan publishing is
  uniformly the issue-comment transport, with no synchronous approval.
- The `plan.approval_gate` config knob proposed in #791 is NOT introduced —
  the gate model is uniform, not configurable per repo.

### Reviewer-of-record invariant (preserved)

ADR-027's invariant that codex remains the reviewer of record stays in
effect. Reviews route through `gc_codex_review`, `gc_codex_verify_finding`,
and `gc_codex_architecture_preflight` regardless of which agent runtime
drives the workflow.

### Pre-push review cycle state

Pre-push `gc_codex_review` runs with `uncommitted=true` are the canonical
Codex review step. The later post-push invocation is retained only as a
tool-layer defense-in-depth path for direct callers; the `/implement` skill
must not drive a second Codex review after the first push. Merge-commit drift
relative to the target branch is covered by CI, integration tests, and
SonarCloud, not by a duplicate Codex pass.

Because the workflow now has one Codex review step instead of two, the
`gc_codex_review` hard cap is three cycles for both pre-push and post-push
tool entrypoints. `gc_codex_verify_finding` remains capped at two calls per
finding because verification loops are per-finding, not whole-review cycles.
Any older workflow text, issue prose, or ADR amendment that refers to a
five-cycle Codex cap, a hard two-cycle `gc_codex_review` cap, or two Codex
review steps is stale and must not drive implementation without a new ADR
amending GC-O007.

Because the pre-push review has no PR issue number, its durable cycle state is
anchored to the GitHub issue resolved at workflow Step 1. The marker records
the current branch name as audit-only context — the cap counter itself is
keyed by issue alone, so a branch rename on the same issue cannot reset the
counter. Earlier drafts of this ADR had the cap keyed by `(issue, branch)`,
but PR #800 review (cycle 2) flagged that as a bypass: a noncompliant agent
could rename `<issue>-x` to `<issue>-x-2` and start fresh. Per-issue keying
closes that path. Legitimate "abandon and restart on a new branch" remains
available via the user-authorized `override_cap=true` + `override_reason`
path; the override marker stays distinguishable from regular cycle markers in
the audit trail.

The marker belongs to the same issue-thread marker family as plan, phase,
review-cycle, and verify-cycle markers. Implementations must reuse the
existing issue-comment read/post helpers, marker parser/evaluator pattern, and
structured refusal result style; they must not add a local state file, git
notes, database row, Temporal state, or driver-local counter for this cap.

### Codex findings issue-thread record

After every successful `gc_codex_review` cycle, the MCP server must post a
human-readable findings record to the resolved GitHub issue thread. That
comment is the durable record for "what Codex said in cycle N"; agent-written
decision summaries remain a separate record of what the agent did with each
finding.

The findings record must preserve the existing machine-readable cycle marker
contract and include:

- cycle number, cap, reviewer names, and review mode (`pre-push` or
  `post-push`);
- the verbatim `core_review_text` and `security_review_text` returned by the
  reviewers;
- for post-push reviews, every inline PR review comment URL that was created.

Post-push inline PR review comments still exist for anchored human review.
The issue-thread findings comment is additive, not a replacement for inline
comments. If posting the issue-thread findings record fails, the review run is
not durable and must fail fast with a structured
`review_comment_post_failed` result while preserving the review text and
finding metadata in the returned payload.

Implementations must route this through the existing host-side GitHub posting
boundary in `gc_codex_review`: use the same `gh api` issue-comment helpers,
secret-content guardrails, parser/evaluator result envelopes, and
issue-resolution logic already used for phase markers, cycle markers, and
inline PR comment posting. Do not let Codex call `gh` directly, do not create a
second GitHub client abstraction, and do not make agent prose the only source
of truth for review findings.

The cap mechanism is an audit / discipline gate, not a security boundary. A
fully noncompliant or compromised agent with shell access has many paths to
bypass — the cap narrows the most likely accidental-bypass path (branch
rename) but does not protect against all attacks. The user's PR merge
remains the only synchronous human gate.

## Consequences

### Positive

- One synchronous gate instead of two: the workflow proceeds asynchronously
  after `/implement` is invoked. The user sees the result on PR ready-to-merge
  rather than being interrupted mid-run.
- Issue thread becomes a single durable surface that survives PR merge/close,
  unlike PR review comments which are tied to the PR's lifecycle.
- Plan, findings, and decisions are colocated and time-ordered, making
  retrospective audit easier than scraping multiple surfaces.
- Aligns the four current repos on a single gate model — pulsar's prior
  divergence becomes the new norm rather than a fork.

### Negative

- Removing the synchronous plan-approval gate transfers accountability for
  early-stage course correction onto the codex preflight + plan content
  itself. If preflight is weak or the plan is wrong, the workflow proceeds
  to TDD against a flawed plan, and the cost of rework is higher than
  catching it at plan-approval time.
- Issue threads become longer. Plans + finding summaries + decisions can
  accumulate to hundreds of lines per `/implement` run.

### Risks

- **Agent silence on finding decisions.** Without a human approval gate,
  agents could silently mark findings wontfix without rationale.
  Mitigation: every finding decision is mandated as an issue comment with
  rationale; the codex `verify_finding` flow already enforces this on PR
  comments. Issue-thread duplication is the new explicit step.
- **Drift in plan quality.** Plans that previously got rubber-stamped will
  now drive TDD without that rubber-stamp gate. If plan quality erodes,
  it'll show up in larger review-fix loops. Counter: the codex preflight
  + plan-rules-from-`.ground-control.yaml` already shape plans before TDD;
  this is the same input quality, just without a synchronous human pause.
- **Audit retrospective.** When a PR ships with bad code, the question
  "did the human approve this?" no longer has a yes/no. Counter: the issue
  thread is the durable record; reviewers can trace what was known and
  what was decided.

## Migration

- **This PR (#791)** lands ADR-029 simultaneously with the canonical
  `skills/implement/SKILL.md` rewrite that follows the new gate model.
- **In-flight work** under the old gate model finishes under the old gate.
  This PR itself is being executed under the deprecated plan-approval gate
  (which the user explicitly granted before this ADR was authored). Future
  `/implement` runs use the new gate.
- **GC-O007 statement** is amended via `gc_update_requirement` in this PR
  to reflect one human touchpoint and the issue-thread durable-record
  model. The prior statement remains in `gc_get_requirement_history` for
  audit.

## Non-Goals

- Eliminating PR review or merge approval. PR merge stays a synchronous
  human gate.
- Making the workflow fully autonomous. The user still owns merge and
  remains accountable for ratification of the work.
- Re-implementing `EnterPlanMode` as a Codex feature. Codex-driven
  `/implement` runs use the same `gh issue comment` plan transport as
  Claude-driven runs.

## Related Requirements

- GC-O007 Gated Agentic Development Loop (statement amended)
- GC-O009 Workflow Orchestration via Temporal (the durable end state)

## Related ADRs

- ADR-021 Gated Agentic Development Loop (amended)
- ADR-027 Agent-Neutral Implement Workflow Packaging (companion ADR; same PR)
- ADR-028 Temporal Workflow Orchestration Boundary (forward-looking for
  GC-O009)
