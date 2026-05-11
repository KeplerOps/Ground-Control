# Changelog Fragments Workflow Preflight

Issue #848 replaces per-PR hand edits to `CHANGELOG.md` with towncrier-style
fragments in Ground-Control-aware repositories. This note is architecture
preflight guidance for the implementation. It does not implement the fragment
workflow and does not replace the canonical `/implement` skill, MCP repository
context helpers, or repo-native policy gates.

## Architecture Boundaries

- Keep the workflow source agent-neutral. The canonical surface is
  `skills/implement/SKILL.md`; Claude-only hooks or host-local skill copies are
  install targets and safety rails, not the source of truth.
- Treat repo bootstrapping as an MCP/library concern. The current starter
  template lives in `buildSuggestedGroundControlYaml()` and is validated by
  `parseGroundControlYaml()` / `getRepoGroundControlContext()` tests. Do not add
  a separate unvalidated repository-template generator for changelog fragments.
- Keep changelog policy in the existing repo-native policy layer (`bin/policy`,
  `tools/policy/checks.py`, `make policy`) and `.gc/plan-rules.md`. Do not rely
  on user-level hooks as the only enforcement layer.
- Preserve ADR-021/ADR-027/ADR-029 gate semantics. This issue changes the
  changelog artifact expected by plans and completion gates; it must not change
  issue-thread plan posting, Codex review routing, traceability reconciliation,
  or the one-human-touchpoint model.
- Keep release collation repo-local. The shared template should document the
  recommended `towncrier build` invocation and marker contract, but the trigger
  remains each repository's release process.

## Cross-Cutting Concerns

- **Configuration validation:** any new config-supplied path or branch value must
  go through the existing `.ground-control.yaml` parser patterns: unknown-key
  rejection, repo-relative path validation, and realpath containment for paths
  that will be read or written. If no new config field is needed, do not add one.
- **Command execution:** invoke external tools with argv-style execution from
  trusted workflow scripts. Do not compose shell strings from issue numbers,
  fragment names, base refs, or repository config.
- **Path safety:** fragment paths are repo-local under `changelog.d/`. Validate
  the `<issue>` / `+<slug>` stem and `<type>` vocabulary before any automation
  creates, checks, or consumes fragments. The allowed type vocabulary is the
  Keep-a-Changelog set: `security`, `added`, `changed`, `deprecated`,
  `removed`, `fixed`.
- **Policy and tests:** add structural checks where the contract is mechanical:
  skill prose no longer requiring direct `CHANGELOG.md` edits, starter-template
  files being emitted or documented together, plan rules naming fragments, and
  any optional towncrier check staying soft-fail for legitimate no-fragment
  changes.
- **Docs and workflow sync:** update `docs/DEVELOPMENT_WORKFLOW.md`, PR-template
  checklist language, `.gc/plan-rules.md`, and relevant ADR text when they still
  claim source changes must directly edit `CHANGELOG.md`.

## Guardrails

- Do not conflate a changelog fragment with a release entry. Fragments are
  per-PR inputs; `CHANGELOG.md` is generated or collated at release time.
- Do not make every PR require a fragment. CI-only and docs-only diffs may
  legitimately have no release note. There is no "pure refactor" carve-out:
  path-based enforcement cannot distinguish a behavior-preserving refactor
  from a feature change, so refactors under application source still file a
  fragment. (The implementation in `tools/policy/checks.py` and
  `.claude/hooks/verify-implementation.sh` matches this contract; an earlier
  draft of this note carried the older carve-out and was corrected during
  the codex review for #848.)
- Do not make `CHANGELOG.md merge=union` the primary design. It is
  belt-and-suspenders for release collation commits and straggler hand edits.
- Do not add a new schema, exception hierarchy, database table, service layer,
  or Ground Control artifact/link type for changelog fragments.
- Do not vendor towncrier or add runtime dependencies to the backend/frontend for
  this workflow-only concern. If a check is added, prefer ephemeral tooling such
  as `uvx towncrier check`.
- Do not let towncrier's optional PR check fail hard unless the repository
  explicitly opts into a stricter release-note policy; the issue calls for
  soft-fail because no-fragment changes are valid.

## Extensibility

The reusable seam is the fragment convention, not a new workflow engine:
`changelog.d/` as the directory, a fixed fragment-type vocabulary, and a
release-time collation command documented in each repo. If a future repository
needs a different changelog directory or base branch for checks, add that as a
validated `.ground-control.yaml` field only when the need exists; until then,
keep the convention static and documented.

## Non-Goals

- No release automation trigger in the shared template.
- No hard CI requirement for fragments.
- No migration of historical `CHANGELOG.md` content beyond adding the
  towncrier marker and direct-edit warning.
- No changes to application auth, persistence, API contracts, traceability
  semantics, or review-loop mechanics.
