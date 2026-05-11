# Implement Cost Routing and Tool Surfaces Preflight

Issue #868 reduces `/implement` run cost through per-step model routing,
deterministic MCP-rendered durable records, and per-step telemetry. This note
is architecture preflight guidance for that implementation. It does not
implement the routing matrix, MCP tools, telemetry writer, policy checks, ADR
amendment, or SKILL changes.

## Architecture Boundaries

- Preserve GC-O007's gate contract. This work may reduce token cost and move
  templated prose into deterministic tools; it must not add a second human
  touchpoint, reintroduce plan approval, add a post-push Codex review pass, move
  traceability/status transitions before reviews, or weaken the zero-deferral
  rule.
- Keep `skills/implement/SKILL.md` as the canonical agent-neutral workflow
  source. Host-local Claude/Codex installs are distribution targets, not
  independent workflow definitions.
- Keep `.ground-control.yaml` and `gc_get_repo_ground_control_context` as the
  repository configuration boundary. Do not parse workflow config, docs paths,
  telemetry paths, model routing, or quality commands from skill prose or
  driver-local files with ad hoc code.
- Keep the MCP server as the privileged side-effect boundary for GitHub writes
  made from agent-authored structured input. New record-rendering tools must
  validate input server-side, render the canonical Markdown, post through the
  host-side `gh api` path, and return structured success/failure envelopes.
- Treat telemetry as operational measurement, not workflow state. The GitHub
  issue thread remains the durable record for plan, findings, decisions, phase
  markers, review-cycle markers, and final report evidence.
- Keep the issue-thread marker family as the only bridge-state persistence
  model until GC-O009's Temporal workflow owns the loop end to end. Telemetry
  files, local run logs, subagent handoff data, or PR body text must never
  become counters or gate state.
- Treat ADR-029 as dispositive when older workflow text conflicts. In
  particular, the current Codex review cap is three cycles; older hard-2
  wording in ADR-027-era text is stale and must not be copied into routing,
  telemetry, or summary tooling.

## Cross-Cutting Concerns to Reuse

- **Repo/config validation:** reuse `ensureGitRepo`, `parseGroundControlYaml`,
  `resolveRepoRelativePath`, `assertRealpathInRepo`, and the existing
  unknown-key rejection/defaulting pattern for any new config field. Path-valued
  fields must be repo-relative, reject `..`/absolute paths, and use realpath
  containment when the implementation reads or writes files.
- **GitHub posting:** reuse `getOwnerRepo`, paginated `readIssueCommentBodies`,
  `postPhaseMarker`/issue-comment posting patterns, marker-shaped-text
  escaping, GitHub comment-size caps, and the existing `gh api` argv-style
  execution. Do not let the driver agent call `gh issue comment` directly for
  the new durable-record surfaces.
- **Tool schemas:** add Zod schemas in `mcp/ground-control/index.js` and pure
  validation/rendering helpers in `mcp/ground-control/lib.js`, matching the
  `gc_post_implementation_plan` and `gc_codex_review` split. Do not duplicate a
  schema in the SKILL, policy code, and MCP handler.
- **Error envelopes:** use stable MCP refusal/result shapes with `ok`, `error`,
  `message`, ids, and `next_action` fields for expected workflow failures.
  Backend REST calls still flow through `RequestError` / `parseErrorBody`;
  GitHub transport failures should surface without stack traces or secrets.
- **Secret handling:** reuse `detectSensitiveBodyContent` before publishing any
  model- or agent-controlled body. New tools must not publish prompts,
  completions, environment dumps, bearer tokens, private keys, provider API
  keys, Sonar tokens, or raw command output that may contain secrets.
- **Policy/docs sync:** workflow-surface changes must keep
  `architecture/policies/adr-policy.json`, `docs/DEVELOPMENT_WORKFLOW.md`,
  `docs/WORKFLOW.md`, ADR-021/ADR-027/ADR-029, `.gc/plan-rules.md`, and
  `skills/implement/SKILL.md` consistent. `make policy` is the repo-native
  guardrail; live Ground Control policy sync/checks apply when policy or ADR
  workflow surfaces change and a GC instance is reachable.
- **Testing:** keep pure renderers/parsers cheap to test in
  `mcp/ground-control/lib.test.js`. Policy or workflow-sync behavior belongs in
  `tools/tests/test_policy.py`; do not rely on prose-only verification.
- **Backend REST boundary, if touched later:** reuse `ApiSecurityConfig`'s
  ADR-026 path matrix, Bean Validation on request records, `ProjectService`
  scoping, `GlobalExceptionHandler` / `ErrorResponse`, `RequestLoggingFilter`,
  `ActorFilter`, `ActorHolder`, and `@ConfigurationProperties` with
  `ignoreUnknownFields = false`. Do not introduce a parallel auth check,
  exception envelope, actor source, or env parser for workflow bridge endpoints.

## Security Layers In Scope

- **MCP tool argument schemas:** every new tool needs positive integer issue/PR
  ids, bounded enum-like fields (`reviewer`, `decision`, `change_class`,
  routing tier/model id), bounded arrays/strings, and rejection of unknown or
  unsupported decision values. `defer` must not be accepted as a decision.
- **Repository containment:** telemetry file paths under `.gc/telemetry/` must
  be derived from validated issue number plus a sanitized/encoded branch token;
  never let a branch name or tool argument become a raw path segment.
- **OS/process exposure:** invoke `gh`, `git`, and summary commands with argv
  arrays. Do not put Sonar/GitHub/provider tokens in command-line arguments,
  telemetry records, PR bodies, issue comments, or error messages.
- **GitHub durable-record posting:** render comments in MCP from structured
  input, apply sensitive-content filtering before POST, enforce GitHub body-size
  limits or chunking, and return `*_post_failed` envelopes when the issue-thread
  record did not land.
- **Config shape:** if routing/telemetry becomes configurable, it must extend
  the existing `.ground-control.yaml` parser with strict validation. The initial
  static matrix should still have one canonical table, not scattered prose.
- **Error leakage:** expected gate failures return structured refusal envelopes;
  unexpected process/transport failures should include actionable stderr only
  after stripping secrets and must not expose stack traces or raw environment.
- **Backend error envelope, if REST endpoints are introduced:** expected domain
  failures use the existing `GroundControlException` subclasses and serialize
  through `GlobalExceptionHandler` as `ErrorResponse`; unexpected failures log
  server-side with MDC and return the stable `internal_error` envelope.

## Whole-Repo Surfaces In Scope

- `.ground-control.yaml` and `.gc/plan-rules.md` for repo configuration,
  quality commands, docs paths, and plan-rule context.
- `skills/implement/SKILL.md`, `docs/WORKFLOW.md`,
  `docs/DEVELOPMENT_WORKFLOW.md`, ADR-021, ADR-027, ADR-028, ADR-029, and
  `architecture/policies/adr-policy.json` for workflow contract drift.
- `mcp/ground-control/index.js`, `mcp/ground-control/lib.js`,
  `mcp/ground-control/lib.test.js`, and `mcp/ground-control/README.md` for MCP
  schema, renderer, GitHub side-effect, and tool-catalog behavior.
- `bin/policy`, `tools/policy/checks.py`, `tools/policy/deferral_cases.json`,
  `tools/tests/test_policy.py`, and `bin/check-pr-body` for completion-gate and
  PR-body policy reuse.
- `.claude/hooks/block-defer-language.py`,
  `.claude/hooks/verify-implementation.sh`, `bin/install-skills.sh`, and
  `scripts/bootstrap-claude-workflow.sh` for host-local mirror behavior; these
  are not the source of truth, but workflow-surface edits can make them drift.
- `Makefile` targets `make policy`, `make check`,
  `make sync-ground-control-policy`, and `make policy-live` for repo-native and
  live policy verification.
- If bridge work touches product REST or UI later:
  `backend/src/main/java/com/keplerops/groundcontrol/shared/security/`,
  `backend/src/main/java/com/keplerops/groundcontrol/api/GlobalExceptionHandler.java`,
  `backend/src/main/java/com/keplerops/groundcontrol/shared/web/ErrorResponse.java`,
  `backend/src/main/java/com/keplerops/groundcontrol/shared/logging/`,
  `backend/src/main/java/com/keplerops/groundcontrol/shared/web/ActorFilter.java`,
  frontend `src/lib/api-client.ts`, and frontend `src/types/api.ts`.

## Extensibility Guardrails

- The model-routing seam is a stable workflow step id plus a provider-neutral
  capability tier, with a driver-specific concrete model selector at the edge.
  Do not bake one provider's product names into GC-O007 semantics or into every
  step body. A future Codex, Claude Code, or Temporal worker should consume the
  same step id and tier contract even if concrete model ids differ.
- The durable-record tool seam is "structured input -> canonical Markdown ->
  issue/PR post + marker/result envelope." `gc_post_decision_record`,
  `gc_post_final_report`, and `gc_render_pr_body` should share renderer/posting
  primitives so adding a later record type does not clone GitHub, marker,
  sensitive-content, or body-size logic.
- The telemetry seam is append-only JSONL with a small versioned record shape:
  step id, capability tier/concrete model, token counts when available, wall
  time, timestamp, issue, branch, and outcome. Provider-specific raw usage
  payloads should be normalized before logging, not persisted verbatim.
- Temporal compatibility means deterministic surfaces become future activities;
  it does not mean introducing Temporal state, queues, activity DSLs, or worker
  code in this bridge PR.

## Gotchas and Anti-Patterns

- Do not conflate cost telemetry with compliance evidence. The issue thread and
  Ground Control traceability remain the audit record; telemetry can be missing
  or partial without redefining gate completion unless policy later says
  otherwise.
- Do not create a second workflow schema, second marker family, local state
  counter, database table, git note, or branch-keyed cap to support routing or
  telemetry.
- Do not use free-form agent prose for decision records, final reports, or PR
  bodies once the deterministic tool surface exists. The SKILL should pass
  structured inputs and use the returned rendered body/comment URL.
- Do not duplicate PR-template policy logic inside `gc_render_pr_body`.
  Rendering must be built against the same policy contracts that `bin/policy`
  enforces, and policy tests should catch drift.
- Do not let model-routing delegation bypass the reviewer-of-record invariant:
  architecture preflight, Codex review, and verify-finding still route through
  `gc_codex_architecture_preflight`, `gc_codex_review`, and
  `gc_codex_verify_finding`.
- Do not allow subagent handoff to silently drop findings or decisions. Every
  finding must receive a `fix`, `wontfix` with user authorization, or
  `not-applicable` with rationale record on the issue thread.
- Do not log prompts, full issue bodies, full PR bodies, reviewer text, diffs,
  command output, or secret-bearing environment data in telemetry.

## Non-Goals

- No change to the GC-O007 gate model, Codex review cap, phase ordering, or
  single human touchpoint.
- No Temporal implementation, workflow engine, queue, or durable execution
  store.
- No new backend REST controller, database table, JPA entity, exception
  hierarchy, or API DTO solely for cost telemetry or deterministic comment
  rendering.
- No provider-specific model policy embedded in Ground Control requirements.
- No replacement for `make policy`, `make check`, SonarCloud, traceability
  reconciliation, or the existing MCP review tools.
