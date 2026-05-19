# AI Review Recalibration Preflight

Issue #931 recalibrates architecture preflight, Codex review, and
test-quality review so they can return judgment instead of mechanically
padding findings. This note is architecture preflight guidance only. It does
not implement the MCP schema changes, prompt rewrites, CI tooling, repo
vocabulary block, property tests, or SonarCloud ADR draft.

## Architecture Boundaries

- Keep `.ground-control.yaml` and `gc_get_repo_ground_control_context` as the
  repository configuration boundary. The new `architecture.vocabulary` block is
  repo-declared input, not Ground Control hardcoded vocabulary.
- Keep `mcp/ground-control/lib.js` as the canonical parser, prompt builder,
  renderer, and runner home for workflow tools. `mcp/ground-control/index.js`
  should expose only the Zod tool schema and thin handler wiring.
- Keep review outputs as structured tool envelopes. Do not create a separate
  "review judgment" schema in skill prose, policy Python, docs, or GitHub
  comments that can drift from the MCP validator.
- Preserve ADR-027/ADR-029/ADR-036 boundaries: Codex is reviewer of record,
  the MCP server owns privileged GitHub writes, the issue thread is durable
  workflow state, and decision records still go through
  `gc_post_decision_record`.
- Treat the reviewer-axis split as a prompt/rubric partition inside the same
  review tool unless the user explicitly approves it at plan time. If approved,
  use section-level caps and one verdict envelope rather than independent tools
  that can disagree on workflow state.
- Keep the SonarCloud gate recalibration to an ADR draft in this issue. Do not
  change live SonarCloud gates, `sonar-project.properties`, policy checks, or
  CI failure behavior for Sonar in this PR.

## Cross-Cutting Concerns to Reuse

- **Config parsing:** extend `parseGroundControlYaml`,
  `normalize*Config`, `buildSuggestedGroundControlYaml`, and
  `getRepoGroundControlContext` in `mcp/ground-control/lib.js`. Preserve strict
  unknown-key rejection, optional-section defaults, non-empty string checks,
  and repo-relative path validation for any path-valued vocabulary entries that
  are opened or realpath-checked.
- **Review prompt surfaces:** update `buildCodexArchitecturePreflightPrompt`,
  `buildCodexReviewCorePrompt`, `buildCodexSecurityReviewPrompt`, and
  `buildTestQualityReviewPrompt`. Do not add prompt text in
  `skills/implement/SKILL.md` that the MCP tools cannot enforce.
- **Review parsing and envelopes:** evolve `parseCodexReviewFindingsTail`,
  `parseReviewerTailSafely`, `parseTestQualityReviewFindings`, and the
  `runCodexReview` / `runTestQualityReview` result shapers. Existing
  `next_action`, cap, override, parse-error, post-failure, and marker behavior
  should remain stable while the payload gains `verdict`,
  `architectural_read`, `blocking`, and capped `notes`.
- **Decision records:** extend `validateDecisionRecordInput`,
  `buildDecisionRecord`, and `runPostDecisionRecord` instead of adding another
  durable comment renderer. Clean review cycles must remain first-class records,
  now carrying `verdict: ship` rather than relying on `findings: []` alone.
- **GitHub side effects:** reuse `ensureGitRepo`, `getOwnerRepo`, issue-comment
  readers, marker families, marker-shaped-text rejection, `gh api` argv-style
  posting, GitHub body-size checks, partial-failure envelopes, and
  `detectSensitiveBodyContent`.
- **Policy/docs sync:** workflow-surface edits must keep
  `skills/implement/SKILL.md`, `skills/quickfix/SKILL.md` if affected,
  `docs/DEVELOPMENT_WORKFLOW.md`, `docs/WORKFLOW.md`,
  `architecture/policies/adr-policy.json`, `tools/policy/checks.py`, and
  `tools/tests/test_policy.py` in sync. `make policy` remains the completion
  guardrail.
- **Test tooling:** jqwik is already present in `backend/build.gradle.kts` and
  property tests already live under `backend/src/test/java/...PropertyTest`.
  OSV/Trivy advisory CI jobs already exist in `.github/workflows/ci.yml`; if
  the issue upgrades dependency scanning to a blocking "no new CRITICAL CVEs in
  diff" rule, build on those jobs and lockfile validation rather than adding a
  new scanner lane.

## Security Layers In Scope

- **MCP argument schemas:** new fields must be closed enums or bounded arrays:
  `verdict in {ship, ship-with-fixes, don't-ship}`, capped `notes`, existing
  reviewer enums, positive issue/PR/cycle ids, and no `defer` decision path.
- **Structured model output parsing:** reject malformed verdict payloads. A
  missing `architectural_read`, unswept `one-off`, over-cap notes list, class
  finding without instances, or `don't-ship` without a structural blocking
  reason should be a parse/validation failure, not silently normalized.
- **Config shape:** `architecture.vocabulary` must be optional and default to
  safe workflow-level negative space and examples. When present, reject unknown
  keys and wrong types under `patterns`, `canonical_helpers`,
  `boundary_contract`, `binding_adrs`, and `anti_recommendations`.
- **Repository containment:** `example_path` and helper `path` values are
  repo-relative documentation pointers. If implementation reads them, reuse
  `resolveRepoRelativePath` and `assertRealpathInRepo`; reject absolute paths,
  `..`, and symlink escapes.
- **Secret handling:** any prompt-derived, model-derived, or agent-controlled
  review body posted to GitHub must pass through `detectSensitiveBodyContent`.
  Do not post raw prompts, raw diffs beyond the existing bounded findings
  record behavior, environment data, provider keys, GitHub tokens, Sonar
  tokens, or scanner logs.
- **OS/process exposure:** keep `codex`, `claude`, `gh`, `git`, Gradle,
  npm, OSV, Trivy, and Pitest calls argv-based. Do not put tokens, prompts,
  diff bodies, branch-derived paths, or scanner secrets in command-line
  arguments or durable comments.
- **Error envelopes:** expected config, parse, cap, marker, scanner, and
  posting failures should return structured `ok: false` envelopes with stable
  `error`, `message`, and `next_action` fields. Avoid stack traces, raw stderr
  dumps, or exception hierarchy churn.
- **CI secret surfaces:** blocking dependency/SBOM scans must run on
  `ubuntu-latest` PR-safe jobs with least privileges and without repository
  secrets. Keep `policy-live` and pack-registry jobs restricted to trusted refs
  as they are today.

## Extensibility Guardrails

- The vocabulary seam is `cfg.architecture.vocabulary`: repo-specific dialect
  in, filtered applicable vocabulary out of preflight, same vocabulary back
  into review prompts. A future repo should adopt the affordance by editing its
  own `.ground-control.yaml`, not by patching Ground Control prompt strings.
- The review outcome seam is a versionable verdict envelope:
  `verdict`, `architectural_read`, `blocking`, and capped optional `notes`.
  Future axes or reviewer examples should add fields or subsections inside this
  envelope, not create another findings-only path.
- The one-off/class seam needs sweep evidence on every one-off and discovered
  instances on every class. Keep this as machine-validated payload data so
  future reviewers and policy checks can consume it without parsing prose.
- The negative-space seam is workflow defaults plus optional
  `architecture.vocabulary.anti_recommendations`. This prevents each repo from
  copying the whole anti-rubric just to add one local anti-pattern.
- Pitest should enter through a dedicated Gradle configuration/target and
  `make test-quality`; thresholds should be parameters, initially scoped to
  changed classes, so later calibration does not require rewriting the workflow
  contract.

## Gotchas and Anti-Patterns

- Do not conflate `notes` with merge blockers. Notes carry no decision and must
  not feed the fix-until-clean loop.
- Do not preserve a hidden `findings[]`-only contract under new field names.
  The schema must let a clean review say `verdict: ship` with no notes.
- Do not hardcode Ground Control's own helpers, ADRs, or design vocabulary in
  generic workflow prompts. This repo's vocabulary belongs only in its
  `.ground-control.yaml`.
- Do not create duplicate anti-rubric lists in docs, skills, tests, and prompt
  builders. Keep one canonical prompt source with tests asserting key phrases
  and caps.
- Do not turn few-shot examples into policy. They calibrate tone; validators
  enforce shape.
- Do not add abstractions for two or three prompt lines, tiny scanner wrappers,
  or one-off CI invocations. The issue's point is reducing bloat, and the
  implementation should demonstrate that restraint.
- Do not replace static analyzers with LLM findings. Sonar, Checkstyle,
  SpotBugs, Error Prone, gitleaks, OSV/Trivy, Pitest, jqwik, and `make policy`
  each own different gates.
- Do not broaden jqwik or Pitest into a whole-codebase mandate in this PR.
  Choose the high-payoff surfaces named by the issue and keep the initial gate
  intentionally loose.

## Non-Goals

- No implementation of issue #931 in this preflight note.
- No new backend REST controller, database table, JPA entity, exception
  hierarchy, or product UI for review verdicts or vocabulary.
- No new workflow engine, Temporal state, local counter, git note, or database
  persistence model for review outcomes.
- No removal of class-vs-one-off classification, review caps, zero-deferral
  policy, issue-thread durable records, or the single PR-merge human
  touchpoint.
- No live SonarCloud gate activation in this issue; only the ADR draft belongs
  in scope.
