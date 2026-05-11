# No-Deferral Enforcement Preflight

Issue #830 asks for mechanical enforcement of ADR-029's finding-disposition
contract: reviewer findings must be fixed, marked `wontfix` with explicit user
authorization, or marked `not-applicable` with rationale. `defer` is not a
valid disposition.

This note is preflight guidance for the implementation. It does not implement
the hook, policy check, ADR amendment, or skill update by itself.

## Architecture Boundaries

- Keep ADR-029 as the semantic source of truth for dispositions. The hook,
  `bin/policy`, and skill prose should enforce the same ADR language; they must
  not define a second decision vocabulary.
- Reuse the existing issue-thread gate model. Durable evidence lives on the
  GitHub issue thread through the existing plan, findings, and decision-comment
  pattern. Do not introduce local marker files, git notes, a database table, or
  driver-local state for finding disposition.
- Treat the Bash hook and repo policy as two layers over the same behavior:
  the hook prevents bad `gh` calls before they run, and `bin/policy` catches
  bad PR/issue text at completion-gate or CI time. Neither layer replaces the
  other.
- Keep the policy implementation inside the existing stdlib-only Python policy
  surface under `tools/policy/`, with unit tests in `tools/tests/test_policy.py`.
  `bin/policy`, CI, pre-commit, and `make policy` already use that path.
- Keep the Claude hook under `.claude/hooks/` and install it through
  `scripts/bootstrap-claude-workflow.sh`'s explicit `WORKFLOW_HOOKS` allowlist.
  User-level workflow hooks are copied to `~/.claude/hooks/`; do not rely on a
  worktree symlink or an unregistered repo-local hook.

## Cross-Cutting Concerns to Reuse

- **Policy violation shape:** use the existing `Violation(code, message,
  details)` rendering pattern in `tools/policy/checks.py` for completion-gate
  failures.
- **PR body plumbing:** extend the existing PR body resolution paths
  (`--pr-body-file`, `--pr-number`, GitHub event payload) rather than adding a
  separate PR-body checker.
- **Hook input contract:** follow `git-merge-guard.py`'s PreToolUse model:
  parse the Claude hook JSON from stdin, inspect `tool_input.command`, write a
  clear denial reason to stderr, and exit `2` for blocked commands.
- **Issue-thread machinery:** when disposition completeness needs durable
  review context, reuse the MCP patterns in `mcp/ground-control/lib.js`:
  `ensureGitRepo`, `getOwnerRepo`, paginated `readIssueCommentBodies`,
  phase/cycle marker parsing, `postCodexReviewFindingsComment`, and
  `buildCodexReviewFindingsComments`. Those helpers already handle repo
  binding, marker-shaped text escaping, GitHub comment-size limits, and
  structured refusal envelopes.
- **Workflow docs and ADR policy:** changes to workflow guardrails should keep
  `architecture/policies/adr-policy.json`, `docs/DEVELOPMENT_WORKFLOW.md`,
  `docs/WORKFLOW.md`, and ADR-021/ADR-029 in sync where applicable.
- **Ground Control context:** if workflow behavior needs repo config, use
  `.ground-control.yaml` via the configured Ground Control context path rather
  than hardcoding project-specific assumptions.

## Guardrails

- Use one reviewed classifier for deferral language where practical, and make
  its context explicit: command kind, target surface, and whether the text is a
  new issue's own scope definition or a completion/closing/reporting surface.
- Do not make a blanket substring check. Phrases such as an issue template's
  `## Out of scope` section can be legitimate on `gh issue create`, while
  "deferred to follow-up PR" is a forbidden disposition even in a new issue.
- Resolve `gh` body text through supported inputs (`--body`, `--body-file`,
  and equivalent short flags). Parse command arguments with `shlex`; do not
  execute or regex-split shell commands to discover body text.
- Bound any `--body-file` read: regular files only, limited size, clear errors
  for unreadable files. The hook should never run `gh` or make network calls.
- Denial messages should quote the workflow contract and route the agent to one
  of the valid paths: fix now, record `wontfix` with explicit authorization, or
  record `not-applicable` with rationale. Do not instruct the agent to file a
  follow-up as an allowed escape hatch.
- Treat existing workflow text carefully. The current PR template contains
  "`gc_run_sweep` reviewed or intentionally deferred with reason"; a naive
  scanner will flag the template and existing well-formed PR bodies. The
  implementation must either amend that workflow language in the same change or
  narrowly classify that historical gate phrase so it does not mask real
  finding deferrals.
- Text scanning cannot prove that an agent silently dropped a finding. If the
  implementation attempts mechanical silence detection, it should reuse
  ADR-029's issue-thread findings records and decision comments rather than
  inferring completeness from PR prose alone.

## Non-Goals

- No new finding-disposition enum, schema, exception hierarchy, service layer,
  persistence model, or workflow state store.
- No replacement for `gc_codex_review`, `gc_codex_verify_finding`, SonarCloud,
  `/review-tests`, or the existing review-loop caps.
- No retroactive cleanup of old issues or PRs with deferral language.
- No broad natural-language moderation layer for every `gh` command. Scope the
  hook to the GitHub issue/PR create, edit, and comment commands named in
  issue #830.
- No implementation-specific bypass list hidden in agent prose. Allowed
  contexts must be encoded in tests so future policy changes are reviewable.
