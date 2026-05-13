# Quickfix Workflow Lane Preflight

Issue #906 introduces a lower-ceremony `/quickfix` lane and changes the
review-loop defaults shared with `/implement`: Codex review cap 3 -> 1,
test-quality review moves pre-push, and test-quality's default cap also becomes
1. This note is architecture preflight guidance only. It does not implement the
new skill, MCP config, cap behavior, policy tests, ADR amendments, or workflow
docs.

## Architecture Boundaries

- Keep GitHub issues as the entry point for both lanes. `/quickfix` may skip
  requirement transitions and heavy planning, but it still starts from an issue
  or UID-resolved issue, creates the bounded branch, applies the in-progress
  label, and records pickup on the issue thread.
- Keep `/implement` and `/quickfix` as workflow lanes over the same mechanical
  guardrails, not two policy systems. `bin/policy`, `make check`, `make
  policy`, PR-title validation, PR-body rendering, no-deferral checks,
  changelog fragments, CI, and SonarCloud remain lane-agnostic.
- Keep `skills/implement/SKILL.md` as the canonical full workflow and create
  `skills/quickfix/SKILL.md` as a smaller sibling, not a forked copy that owns
  duplicated helper logic. The quickfix skill should reference shared
  contracts where possible and spell out only the intentional drops.
- Keep the issue thread as durable workflow state. `/quickfix` replaces the
  implementation-plan marker and final-report tool with lightweight comments;
  it must not introduce local files, telemetry records, PR body text, branch
  names, or driver memory as phase state.
- Keep the MCP server as the enforcement boundary for review caps and marker
  parsing. The cap change is a configurable default pulled from
  `.ground-control.yaml`, not an agent-side "stop after one" convention.
- Treat `--review` as an opt-in review mode for `/quickfix`, not a third
  workflow. When present, it should run the same pre-push Codex and
  test-quality MCP tools and the same decision-record contract as `/implement`,
  with the configured cap defaulting to 1.

## Cross-Cutting Concerns to Reuse

- **Repo/config validation:** extend `parseGroundControlYaml` /
  `normalizeWorkflowConfig` and `gc_get_repo_ground_control_context` for
  `workflow.codex_review.pre_push_cap` and
  `workflow.test_quality_review.pre_push_cap`. Preserve strict unknown-key
  rejection, type checks, and safe defaulting. Do not add a second YAML parser
  or read these knobs from skill prose.
- **Review caps:** reuse the existing marker families and evaluators in
  `mcp/ground-control/lib.js`: `gc:codex-prepush-cycle` and
  `gc:test-quality-review-cycle`. The cap key remains per issue; branch is
  audit context only. Do not create `/quickfix` marker families or branch-keyed
  counters.
- **Review tools:** reuse `gc_codex_review`, `gc_test_quality_review`, and
  `gc_post_decision_record`. A clean review still posts `findings: []` and
  advances only after the decision-record post returns `ok: true`.
- **GitHub side effects:** reuse `ensureGitRepo`, `getOwnerRepo`, paginated
  issue-comment readers, marker-shaped-text escaping, sensitive-content
  filtering, GitHub body-size handling, and argv-style `gh api` execution. Do
  not let agent-authored free prose bypass MCP posting where a canonical tool
  already exists.
- **Workflow publication:** keep `skills/implement/SKILL.md`,
  `skills/quickfix/SKILL.md`, `docs/DEVELOPMENT_WORKFLOW.md`,
  `docs/WORKFLOW.md`, ADR-021, ADR-029, ADR-036, and
  `architecture/policies/adr-policy.json` synchronized. The existing
  `workflow-guardrail-sync` rule may need to include `skills/quickfix/SKILL.md`
  so quickfix guardrails cannot drift silently.
- **Policy gates:** leave `tools/policy/checks.py` and `bin/policy`
  lane-agnostic unless a structural sync check is needed. The issue asks for no
  policy bypass; source-changing quickfix diffs still need changelog fragments
  and PR bodies rendered through `gc_render_pr_body`.
- **Routing/telemetry:** if per-step routing or telemetry is mentioned in
  quickfix, use ADR-036's existing stage/tier and operational-telemetry model.
  Add stable stage keys only where the new lane genuinely needs a distinct
  route; do not overload `/implement` stage names with quickfix-only meaning.

## Security Layers In Scope

- **MCP argument schemas:** cap knobs must be positive integers with bounded
  reasonable ranges, and review tools must continue validating positive
  issue/PR numbers, enum values, non-empty override reasons, and reviewer
  decision values. `defer` remains invalid.
- **Config shape:** `.ground-control.yaml` must reject unknown nested keys and
  wrong types under the new `workflow.codex_review` and
  `workflow.test_quality_review` mappings. Defaults should preserve existing
  behavior for repos without the knobs except where this repo intentionally
  sets cap 1.
- **OS/process exposure:** branch names, PR titles, issue comments, `gh`, `git`,
  `claude`, and SonarCloud calls must keep using argv-style execution. Do not
  put GitHub tokens, Sonar tokens, provider keys, raw prompts, raw diffs, or
  command transcripts in argv, comments, PR bodies, telemetry, or errors.
- **Secret handling:** any MCP-rendered or MCP-posted issue comment, decision
  record, final report, or review findings record must pass through
  `detectSensitiveBodyContent` before publication. Lightweight quickfix close
  comments are still public issue-thread text and need the same discipline.
- **Error envelopes:** expected cap refusals, missing override reasons, posting
  failures, sensitive-content rejections, and phase-gate failures should return
  structured envelopes with `ok`, `error`, `message`, `cap`, `prior_cycles`,
  and `next_action` fields. Do not use thrown control-flow exceptions or leak
  stack traces to agents.
- **Error-envelope leakage:** subprocess failures from `claude`, `gh`, `git`,
  CI polling, or SonarCloud must strip secrets and avoid dumping full
  environment, prompts, or large raw outputs into user-visible messages.

## Whole-Repo Surfaces In Scope

- `.ground-control.yaml`, `mcp/ground-control/lib.js`,
  `mcp/ground-control/index.js`, `mcp/ground-control/lib.test.js`, and
  `mcp/ground-control/README.md` for config schema, tool descriptions, cap
  defaults, marker evaluators, and public MCP behavior.
- `skills/implement/SKILL.md` and new `skills/quickfix/SKILL.md` for the
  canonical agent-neutral workflow instructions.
- `docs/DEVELOPMENT_WORKFLOW.md`, `docs/WORKFLOW.md`, ADR-021, ADR-029,
  ADR-036, and `architecture/adrs/031-codex-review-stopping-model.md` for
  workflow contract wording that currently assumes cap 3 and post-PR
  test-quality placement.
- `architecture/policies/adr-policy.json`, `tools/policy/checks.py`, and
  `tools/tests/test_policy.py` for sync checks over workflow-surface edits.
- `changelog.d/`, `towncrier.toml`, `gc_render_pr_body`,
  `check_pr_body`, `.claude/hooks/block-defer-language.py`, and
  `.claude/hooks/verify-implementation.sh` for guardrails quickfix must not
  bypass.
- `Makefile` targets `make check`, `make policy`,
  `make sync-ground-control-policy`, and `make policy-live` for local and live
  verification after workflow or policy changes.

## Extensibility Guardrails

- The review-cap seam belongs in config as
  `workflow.<reviewer>.pre_push_cap`, defaulting to 1 for this workflow change
  and remaining overrideable per repo. Do not bake `1` into prose, tool
  descriptions, and tests as unrelated literals.
- The quickfix lane should be parameterized by invocation flags. `--review`
  should toggle both pre-push AI-assisted reviews through existing tools; future
  flags such as `--no-close` or `--base` should be possible without rewriting
  the skill's core guardrail list.
- The lane distinction is ceremony level, not artifact type. Future issue-label
  routing can choose between `/implement` and `/quickfix`, but this issue should
  leave routing manual and keep both lanes issue-anchored.
- The workflow-doc seam should allow additional lanes without cloning every
  `/implement` paragraph. Shared invariants belong in one section; lane-specific
  drops belong in the lane section.

## Gotchas and Anti-Patterns

- Do not silently preserve stale cap-3 text in ADR-021, ADR-029, ADR-031,
  ADR-036, tool descriptions, tests, or docs while changing the runtime
  constant to 1. Cap drift is a workflow-contract bug, not cosmetic docs debt.
- Do not move test-quality pre-push by only renumbering the skill. The MCP
  tool description, docs flow chart, policy tests that parse Step 13, and any
  "advance to Step 14" language must move with it or be intentionally retired.
- Do not treat `/quickfix` as permission to skip mechanical gates. The lane
  drops preflight, plan post, default AI-assisted reviews, final report, and
  requirement transitions; it does not drop branch shape, in-progress signal,
  changelog, PR-title validation, PR-body policy, CI, SonarCloud, or user merge.
- Do not duplicate PR title validation, PR body generation, deferral scanning,
  changelog classification, or branch-name rules inside quickfix prose. Point
  the skill at the canonical predicates and tools.
- Do not create a "quick final report" tool unless lightweight close comments
  demonstrably need server-side rendering. A plain issue close comment is
  enough if it follows the no-secret and no-deferral rules.
- Do not let `--review` post findings to the user and stop. Findings are work:
  fix them, post decision records, and re-invoke until clean or cap-refused.
- Do not transition requirements or create new requirement traceability in a
  requirement-free quickfix by default. Only update existing IMPLEMENTS/TESTS
  links when the diff actually moves behavior tied to an existing requirement.

## Non-Goals

- No auto-detection or label-based routing between `/implement` and
  `/quickfix`.
- No policy bypass or separate quickfix policy mode.
- No new workflow engine, Temporal state, local state file, git notes, database
  table, or second GitHub client abstraction.
- No change to the substance of Codex review, test-quality review, SonarCloud,
  CI, PR-title, PR-body, changelog, or no-deferral checks beyond placement and
  configured cap defaults.
- No requirement lifecycle transitions for requirement-free quickfix runs.
