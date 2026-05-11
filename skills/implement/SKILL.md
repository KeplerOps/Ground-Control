---
name: implement
description: End-to-end issue implementation — from plan through merged PR. Agent-neutral (Claude Code, Codex). Parameterized by .ground-control.yaml.
argument-hint: <issue-number | requirement-uid>
disable-model-invocation: true
---

# Implement: $ARGUMENTS

This skill is the canonical, agent-neutral implementation of the Ground Control `/implement` workflow. It runs from either Claude Code or Codex against the same content, with repo-specific values supplied by `gc_get_repo_ground_control_context` (per ADR-027).

The skill handles the entire lifecycle: plan, implement, verify, commit, push, PR, CI, reviews, fix, requirement transitions, traceability reconciliation. **The user's only synchronous touchpoint is PR merge** (per ADR-029). Plans, review findings, and decisions on findings are recorded as comments on the GitHub issue thread so the durable record survives PR merge/close.

**Every run is driven by a GitHub issue.** The issue is the durable artifact that records why the change is being made, what requirements are in scope (if any), and how completion is reviewed. `$ARGUMENTS` may be either a GitHub issue number OR a Ground Control requirement UID; in the UID case the skill finds or creates the matching issue and runs against it. Bug fixes, refactors, dependency updates, and other requirement-free work enter the same workflow via an issue with zero requirements in scope.

**Templating convention.** Where the prose below references a path or value with `{cfg.X|default Y}`, the agent reads `cfg` from `gc_get_repo_ground_control_context`'s response (Step 1) and substitutes `cfg.X` if non-null, else `Y`. Example: `{cfg.docs.adr_dir|default architecture/adrs/}` resolves to whatever the repo's `.ground-control.yaml` declares, falling back to `architecture/adrs/` if unset. This lets one canonical SKILL.md serve every Ground-Control-aware repo.

---

## Phase A: Plan & Implement

### Step 1: Resolve the Issue and Branch

1. Run `pwd` to capture the absolute repository root.

2. Call the `gc_get_repo_ground_control_context` MCP tool with:
   - `repo_path`: absolute path from `pwd`

3. If the tool does NOT return `status: "ok"`:
   - Stop immediately.
   - Tell the user the repository is missing a valid `.ground-control.yaml` at its root.
   - Include the tool's `suggested_ground_control_yaml` in your response and ask the user to create the file before using `/implement`.
   - Do NOT guess the Ground Control project.

4. If the tool returns `status: "ok"`, cache the following fields (referred to as `cfg.X` throughout the rest of this skill):
   - `cfg.project` — used in all Ground Control MCP calls
   - `cfg.workflow.test_command` — the test suite command
   - `cfg.workflow.completion_command` — the full completion gate (falls back to test_command if null)
   - `cfg.workflow.lint_command` — the linter
   - `cfg.workflow.format_command` — the formatter
   - `cfg.sonarcloud` — if set, used by Step 11; if null, SonarCloud is skipped
   - `cfg.rules.plan_rules_content` — if non-null, mandatory plan constraints (Step 4)
   - `cfg.docs.*` — repo documentation paths (Step 3)
   - `cfg.example_paths.*` — repo source/test path examples (Step 4.5)
   - `cfg.requirements.uid_examples` — repo's requirement UID style examples
   - `cfg.cross_cutting_concerns.description` — repo's cross-cutting concern hint (Step 3)

5. **Classify `$ARGUMENTS` as either an issue reference or a requirement UID**:
   - **Issue reference**: a plain integer (`123`), a `#`-prefixed integer (`#123`), or a form like `issue:123`. In all cases, strip to the integer and treat it as the GitHub issue number.
   - **Requirement UID**: anything matching the pattern `<letters>-<letters/digits>` (examples in this repo: `{cfg.requirements.uid_examples|default ["GC-X001", "OBS-042"]}`). Treat the entire string as the UID exactly as provided. Do NOT synthesize or rewrite a project prefix.
   - If the input fits neither pattern, stop and ask the user for disambiguation.

6. **If the input was a requirement UID**, resolve it to a GitHub issue:
   1. Use `gc_get_requirement` with the UID and `cfg.project`. If the requirement does not exist, stop and report it.
   2. Use `gc_get_traceability` with the requirement's UUID. Look for a link with `artifact_type: GITHUB_ISSUE`.
   3. If such a link exists, note the issue number from its `artifact_identifier`.
   4. If no link exists, use `gc_create_github_issue` with the UID and `cfg.project` to create an issue and auto-link it. Note the new issue number.
   - From this point forward, treat the resolved issue number as the authoritative input. The requirement UID becomes a single entry in the `in_scope_requirements[]` list computed below.

7. **Fetch the issue** via `gh issue view <issue-number> --json number,title,body,labels,url`. Cache the full body text.

8. **Parse the `in_scope_requirements[]` list from the issue body.** The convention is a Markdown section whose heading is exactly `## Requirements` (case-insensitive match on the word `Requirements`, any heading level from `##` to `####`). The section contains a bulleted list where each bullet is a Ground Control requirement UID, optionally followed by prose explanation.
   - If the section is present and non-empty, every valid UID bullet becomes an entry in `in_scope_requirements[]`.
   - If the section is present and empty, `in_scope_requirements[]` is empty — this is a bug/refactor/maintenance run with no formal requirements.
   - If the section is absent entirely, `in_scope_requirements[]` is empty.
   - If a UID in the section fails to resolve via `gc_get_requirement`, stop and report the broken reference.
   - If the input was a requirement UID (Step 6), ensure that UID is in `in_scope_requirements[]` — add it if missing, which also means updating the issue body to include a Requirements section.

9. **For each UID in `in_scope_requirements[]`**, fetch the full requirement via `gc_get_requirement` and cache its UUID, title, statement, status, and wave. You will use these for clause verification (Step 4.5), status transitions (Step 15), and traceability reconciliation (Step 16).

10. **Fetch the existing traceability links for the issue** via `gc_get_traceability_by_artifact` with `artifact_type: GITHUB_ISSUE` and `artifact_identifier: <issue-number>`. Cache the result — you will need it to reconcile the issue's relationship to requirements in Step 16.

11. **Switch to the issue's feature branch**: run `gh issue develop <issue-number> --checkout --base {cfg.workflow.base_branch|default dev} --name <issue-number>-<short-slug>`.

    **Always pass `--name` with a short slug.** Without it, `gh` derives the branch name from the full issue title and you get monstrosities like `55-the-ec2-instance-management-permissions-are-overly-broad-with-resource-=-this-allows-terraform-to-manage-any-ec2-instance-in-the-account-not-just-the-ones-it-creates-consider-scoping-these-permissions-using-tags-or-name-patterns-for-example`. These break terminal display, copy-paste, CI logs, GitHub UI breadcrumbs, and any downstream tool that touches the branch name. The total branch name (`<issue-number>-<short-slug>`) MUST be ≤ 50 characters; aim for 30–40.

    To derive `<short-slug>`: read the issue title, pick the 2–4 words that name the *thing being changed* (the noun and the verb on it), drop filler words (`the`, `a`, `with`, `for`, `consider`, etc.), kebab-case them, and ASCII-only (no `→`, no Unicode arrows, no slashes, no equals signs — `gh` and most CI tools tolerate them but they regularly break shell quoting downstream). Examples:

    - Title: "Verify GC-T004 (Risk Treatment Plans): clause-by-clause audit, transition DRAFT→ACTIVE, reconcile traceability" → `--name 825-verify-gc-t004`.
    - Title: "The ec2 instance management permissions are overly broad with resource = *. This allows terraform to manage any EC2 instance in the account, not just the ones it creates. Consider scoping these permissions using tags or name patterns, for example..." → `--name 55-scope-ec2-iam`.
    - Title: "GC-T004 / C8: categorised reassessment triggers + event publisher / listener wiring" → `--name 863-gc-t004-c8-triggers`.

    If the branch already exists (issue was previously picked up), `gh` reuses it and your `--name` is ignored — you stay on whatever branch the previous pickup created.

    Then run `git branch --show-current` and cache that exact string as `<branch>` — sub-step 12 uses it verbatim; do not reconstruct it from the issue number.

    **Validate the actual checked-out branch against the same rule** — if the previous pickup ran before this rule existed (or didn't follow it), the cached `<branch>` will violate the invariant and would otherwise flow through the pickup comment, push, CI breadcrumbs, and the PR head ref. Check `<branch>`:

    - **Compliant** when it starts with `<issue-number>-`, is ≤ 50 characters, contains only `[a-z0-9-]` (no Unicode arrows, no slashes, no `=`), and has at least one `-` after the number prefix. Proceed to sub-step 12.
    - **Non-compliant AND safe to rename.** "Safe to rename" requires BOTH (i) zero commits relative to the configured base branch on the *remote* (the local base may be stale; fetch first), and (ii) no PR exists for this branch. Concretely:
      ```
      git fetch origin {cfg.workflow.base_branch|default dev}
      git rev-list --count origin/{cfg.workflow.base_branch|default dev}..HEAD   # must equal 0
      gh pr list --head <branch> --json number                                    # must equal []
      ```
      When both predicates hold: rename in place AND repair the issue's `LinkedBranch` so the GitHub Development sidebar matches the new branch. The full sequence is mandatory — skipping the LinkedBranch repair leaves the issue pointing at the deleted branch (silently) AND can hand a subsequent `gh issue develop` re-pickup the dangling metadata, which is the original failure mode this post-check exists to prevent:
      ```
      # 1. Local + remote rename.
      git branch -m <new-compliant-name>
      git push origin :<old-branch> <new-compliant-name>

      # 2. Find the existing LinkedBranch id (may be empty if the branch was created manually).
      OLD_LINK_ID=$(gh api graphql -f query='
        query($owner:String!, $repo:String!, $number:Int!) {
          repository(owner:$owner, name:$repo) {
            issue(number:$number) {
              linkedBranches(first: 50) { nodes { id ref { name } } }
            }
          }
        }
      ' -F owner=<owner> -F repo=<repo> -F number=<issue-number> \
        --jq '.data.repository.issue.linkedBranches.nodes[] | select(.ref.name == "<old-branch>") | .id')

      # 3. If a stale LinkedBranch exists, delete it.
      if [ -n "$OLD_LINK_ID" ]; then
        gh api graphql -f query='
          mutation($id:ID!) { deleteLinkedBranch(input:{linkedBranchId:$id}) { issue { id } } }
        ' -F id="$OLD_LINK_ID"
      fi

      # 4. Resolve the issue node id and the new branch HEAD oid (which equals base SHA, since no commits exist).
      ISSUE_NODE_ID=$(gh api graphql -f query='
        query($owner:String!, $repo:String!, $number:Int!) {
          repository(owner:$owner, name:$repo) { issue(number:$number) { id } }
        }
      ' -F owner=<owner> -F repo=<repo> -F number=<issue-number> --jq '.data.repository.issue.id')
      HEAD_OID=$(git rev-parse HEAD)

      # 5. Create the new LinkedBranch.
      gh api graphql -f query='
        mutation($issueId:ID!, $oid:GitObjectID!) {
          createLinkedBranch(input:{issueId:$issueId, oid:$oid}) {
            linkedBranch { id ref { name } }
          }
        }
      ' -F issueId="$ISSUE_NODE_ID" -F oid="$HEAD_OID"
      ```
      Re-run `git branch --show-current` and re-cache `<branch>`. Continue to sub-step 12.

      `<owner>` and `<repo>` are the GitHub owner and repo for the current run (resolvable via `gh repo view --json owner,name`). The mutations are idempotent enough for the post-check's purpose: `deleteLinkedBranch` skipped when no stale link exists; `createLinkedBranch` errors with a recognizable conflict if a link with the same ref already exists (treat as success — the sidebar already points at the right branch).

    - **Non-compliant AND not safe to rename** (commits exist OR a PR exists): STOP. Renaming a published branch with an open PR is a force-push that breaks the PR head ref and any reviews already posted to inline comments. **Apply the in-progress signal first** — execute sub-step 12 in full (label + pickup comment) so the issue is visibly flagged as picked-up-but-paused, then post a SECOND issue comment summarizing the situation (current branch, commit count, PR number if any) and ask the user whether to (a) proceed with the long branch name as a one-shot exception (the rule is not yet repo-policy-enforced; the user owns the call), or (b) close the existing PR, delete the branch, and retry `/implement` from scratch on a fresh compliant branch. Wait for the user's answer; do not push commits while the question is open. The label stays set until Step 18 closes the issue OR the user resolves the situation by retrying on a fresh branch (in which case the new run's Step 1 sub-step 12 re-flags).

    The post-check is the dispositive enforcement of the invariant — `--name` only governs first-time pickups; this check governs every actual run.

12. **Flag the issue as in-progress** so a human scanning the issue list — or another agent — can see at a glance that work is underway. The values below are stated together so a repo that prefers a different visible signal changes one place; pass everything to `gh` as argv, never as a shell-interpolated string:
    - **label** `in-progress` — color `FBCA04`, description `An agent is actively working this issue via /implement`.
    - **pickup comment** — `🛠️ Picked up by /implement — driver <Claude Code | Codex>, branch \`<branch>\`, <ISO-8601 UTC timestamp>.` where `<branch>` is the value cached in sub-step 11 from `git branch --show-current`.

    Run, in order:
    1. `gh label create in-progress --color FBCA04 --description "An agent is actively working this issue via /implement" 2>/dev/null || true` — creates the label if the repo lacks it, and is a harmless no-op otherwise. Do NOT pass `--force`: `gh` treats `--force` as "overwrite", so it would rewrite a repo's existing `in-progress` label's color/description on every `/implement` run — broader than this issue-lifecycle signal. (If creation fails for a real reason — e.g., the token can't manage labels — the next sub-step's `gh issue edit --add-label` surfaces it.)
    2. `gh issue edit <issue-number> --add-label in-progress`.
    3. `gh issue comment <issue-number> --body "<pickup comment>"`.

    This is operational visibility only: the pickup comment is **not** a phase marker, the plan comment, a review-findings record, or the final report, and it gates nothing. If the label create/apply or the comment post fails, surface the failure before continuing to Step 2 — a silent failure defeats the duplicate-work signal. The label is **not** removed on an error path or a partial pause: a run that escalates to the user before Step 18 leaves the issue flagged, because it *was* picked up and the work is paused, not finished. Only Step 18 (issue closure) clears it.

### Step 2: Read the Issue and Gather Context

The issue body was cached in Step 1; re-read it now for labels, comments, and any user discussion. Run `gh issue view <issue-number> --comments` to pull the comment thread too. The issue thread is the durable record (per ADR-029) — including this skill's own plan and decision comments — so historical context lives there. Anchor the plan, clause verification, and review scope on the issue.

### Step 2.5: Run Codex Architecture Preflight

Preflight MUST cover every in-scope requirement, not just the first one. The preflight tool loads exactly one requirement payload per call, so grouped issues that carry multiple UIDs need one call per UID — otherwise codex never sees the statements, rationale, or existing traceability for every requirement after the first, and the returned guardrails will be incomplete.

1. Reuse the absolute repository root from Step 1.
2. **Determine the preflight call set.**
   - If `in_scope_requirements[]` is non-empty, run the call below **once per UID in the list**. Each call preflights a single requirement.
   - If `in_scope_requirements[]` is empty (requirement-free bug/refactor/maintenance run), run the call exactly once with `issue_number` alone and omit `requirement_uid`.
3. For each call in the set, call the `gc_codex_architecture_preflight` MCP tool with:
   - `issue_number`: the issue number from Step 1 (always supplied — it is the authoritative anchor for preflight in both UID-first and issue-first runs)
   - `repo_path`: absolute path from Step 1
   - `project`: `cfg.project`
   - `requirement_uid`: the UID being preflighted in this call (omit entirely on requirement-free runs).
4. After every call in the set has returned, read any ADRs, design notes, or workflow guidance that Codex created or updated. Multiple calls may touch overlapping files — that is fine; treat the union of all guardrails as binding.
5. Treat the returned guardrails, cross-cutting concerns, and non-goals as binding unless they are clearly wrong.
6. Do NOT revert or ignore Codex-added design guidance just because implementation looks possible without it.

### Step 3: Assess Codebase Coverage

Explore the codebase to determine whether the work described in the issue is already covered by existing code:
- Search for relevant classes, methods, tests, and configurations touching the subsystems the issue describes.
- Consult the repository's existing documentation during exploration. Each repo declares its design-context locations in `.ground-control.yaml`. Read:
  - ADRs at `{cfg.docs.adr_dir|default architecture/adrs/}`
  - Architecture overview at `{cfg.docs.architecture_overview|default docs/architecture/ARCHITECTURE.md}` (if present)
  - Coding standards at `{cfg.docs.coding_standards|default docs/CODING_STANDARDS.md}` (if present)
  - Workflow reference at `{cfg.docs.workflow_reference|default docs/DEVELOPMENT_WORKFLOW.md}` (if present)
  - Knowledge base at `{cfg.docs.knowledge_base|default cfg.knowledge.dir|default docs/knowledge/}` (if present). The `knowledge.dir` block (the structured knowledge MCP path) is the source of truth; `docs.knowledge_base` is an override only when documentation prose needs to point somewhere different than the structured knowledge directory. Keeping them aligned is the default.

  Grep these for keywords matching the issue's subject area and read any pages that match before designing changes.
- Check if the described behavior already exists.
- **Before designing new code, inventory the existing cross-cutting concerns the change will need.** Do not skip this — it is the single biggest defense against re-implementing helpers that already exist. Where the repo has supplied a hint:

  ```
  {cfg.cross_cutting_concerns.description|default
    Logger: project's chosen logging library; structured-logging conventions
    Validation / schemas: framework-native validation; schema definitions; runtime validators
    Error types and error-response builders: project's exception/error envelope
    Authentication / authorization helpers
    Configuration loaders and environment-variable access
    HTTP client / DB session wrappers, retry / backoff utilities
    Test fixtures, factory helpers, and mock builders
  }
  ```

  For each concern the new code will touch, find and read the project's existing implementation. Use the existing helper. If you genuinely need a new one, justify the new helper in the plan (Step 4) and note why the existing one didn't fit. Re-implementing what's already there is the failure mode the Step 6.5 codex review is designed to catch — catch it here first so you don't spend a review cycle on it.
- For each requirement in `in_scope_requirements[]`, review its existing traceability links (IMPLEMENTS, TESTS) via `gc_get_traceability` on the requirement UUID. Some or all clauses may already be satisfied.
- Reuse the architecture-preflight guidance from Step 2.5 while assessing existing coverage and planning changes.

### Step 4: Plan and Post to Issue

Per ADR-029, the plan is **published to the GitHub issue as a comment** and the workflow proceeds directly to TDD. There is no synchronous user-approval gate.

1. **If the work is NOT yet complete**: produce a written plan and post it as an issue comment. Identify which files need to be created or modified, what tests to write, and what approach to take.
   - When `in_scope_requirements[]` is non-empty, the plan must cover every clause of every in-scope requirement. When it is empty, the plan must fully address every acceptance criterion in the issue body and any user clarifications in comments.
   - Plans must respect the coding standards and formal methods classification levels.
   - Add or update ADRs as appropriate.
   - Plans whose diff touches application source MUST drop a changelog fragment under `changelog.d/<issue>.<type>.md` (or `changelog.d/+<slug>.<type>.md` for issue-free entries), where `<type>` is one of `security`, `added`, `changed`, `deprecated`, `removed`, `fixed`. CI-only diffs (only `.github/workflows/`) and docs-only diffs (only `docs/**`, `architecture/**`, `README.md`, `CONTRIBUTING.md`, `.gc/**`, `skills/**`, etc.) may ship without a fragment. There is no "pure refactor" carve-out — the policy is path-based, so a behavior-preserving refactor under an application-source path still files a fragment (one-line `### Changed - Internal refactor of X` is fine). Do NOT edit `CHANGELOG.md` directly — release-time `towncrier build` collates fragments. See the repo's `changelog.d/README.md` for the naming convention. Plans must still update the readme and docs as appropriate.
   - Build off existing cross-cutting concerns, code, and patterns (Step 3).
   - Code should be readable, maintainable, and follow the coding standards.
   - Address the concerns a FAANG L6+ engineer would have around security, performance, reliability, and scalability.
   - Avoid reinventing the wheel — use existing libraries and frameworks where appropriate.
   - Simple is better than complex.
   - If `cfg.rules.plan_rules_content` is non-null, treat every bullet in that content as a mandatory plan constraint (repo-specific "plans MUST..." rules).
   - **Design with the repository in view, not just the file you're editing.** The plan must demonstrate the design was considered against all four of: **security** (every cross-cutting layer the change passes through that has a `validate()` / shape-check / parser / policy gate — auth surface, secret-handling, env/config binding shapes, OS-level exposure like a token in process argv, error-envelope leakage — name each layer and how the design satisfies it); **maintainability** (the canonical incumbents — config, script, helper — the change must build on, reuse over new abstraction); **extensibility** (the next obvious change in the same direction; whether the design forecloses it; the seam/parameter that keeps one future variation from re-editing the canonical artifact); **whole-repo view** (the canonical configs, canonical scripts, cross-cutting rules, and host/OS/runtime layers that will see the artifact — enumerate the ones in scope). A design that "sits correctly within the edited file's existing style" but fails a validator outside that file, or that re-implements a canonical incumbent, is the failure this requirement exists to catch *at plan time* rather than at codex-review time. The codex architecture preflight (Step 2.5) is asked the same four questions; reconcile its answers into the plan.

2. **Post the plan to the issue thread** via the `gc_post_implementation_plan` MCP tool with:
   - `repo_path`: absolute path from Step 1
   - `issue_number`: the issue number from Step 1
   - `plan_body`: the full plan as a Markdown string

   The tool refuses unless a `preflight` phase marker exists for this issue (per #794 MVP-2 — `gc_codex_architecture_preflight` writes that marker on success). If you skipped Step 2.5, this gate will refuse the plan post and instruct you to run preflight first; do not work around the refusal by `gh issue comment` directly. The tool also writes a `plan` phase marker so downstream tools can confirm planning happened.

   Cache the returned comment URL for the final report (Step 19).

3. **Do not wait for user approval.** Proceed directly to Step 4.4 (TDD). The issue thread is the durable record of the plan; if the user has feedback they can comment on the issue and the agent can revise mid-flight.

4. **Pause for genuinely subtle questions only.** If preflight, codebase coverage, or planning surfaced a design decision you cannot resolve from context (architectural fork, conflicting ADRs, ambiguous requirement scope), use a clarification mechanism appropriate to the driver (`AskUserQuestion` in Claude Code; equivalent prompt in Codex) BEFORE posting the plan, and finalize the plan with the user's answer. The default is to proceed without asking.

5. **If the work is ALREADY complete** (existing code already satisfies every clause of every in-scope requirement): post a *completion report* on the issue using the same `gh issue comment` mechanism. The report identifies which code satisfies the requirement(s) (with `file:line` references). If `in_scope_requirements[]` is non-empty, verify each requirement is already linked and ACTIVE; if not, continue to Steps 15–16 (transition then reconciliation) to fix the Ground Control state without re-implementing the code.

### Step 4.4: Test-Driven Development (mandatory, with one narrow carve-out)

Once Step 4 has posted the plan, implement using **TDD**. This is not optional except under the documentation-only carve-out below.

**Documentation-only carve-out.** Skip the red-green loop only when ALL of the following hold:

- The entire planned diff is documentation: ADR, README, CHANGELOG (or `changelog.d/<fragment>.md`), skill / workflow prose, design notes, or other static text. A single function, helper, schema field, config knob, behavior change, or other executable line in the diff disqualifies the entire carve-out — the full TDD loop applies, and any documentation in the same diff rides along on the back of the executable behavior's tests rather than triggering a separate carve-out path.
- Every clause of every in-scope requirement AND every acceptance criterion in the issue body is already protected by a **structural gate** — a policy check (e.g., `make policy` rule), schema validator, lint rule, verifier script, structural invariant test, or equivalent automated check that fires on real regression. Reviewer judgment alone (codex review, code review) is not a structural gate; it is a process gate. If you cannot name a structural gate for a clause, the carve-out does NOT apply to that clause; revert to the mandatory loop and write a real test, even if the only behavior you are testing is "the structural invariant exists." If the structural gate is genuinely missing and adding one is in scope, add it (it is the "real fix" path) before declaring the carve-out.
- The plan (Step 4) explicitly declared the carve-out and named the structural gate that protects each clause/criterion.
- A second comment on the issue thread re-states the carve-out and the named structural gate, so the durable record is unambiguous (per ADR-029). One issue comment per `/implement` run is fine; bullets per clause are encouraged.
- A substring or snapshot test against the changed prose ("ADR-007 contains 'AIOPS-ACC-003'") does NOT count as a structural gate. If the only test you can write is one that asserts the doc says what it says, that is the carve-out's failure mode — STOP. Either add a real structural gate as part of this PR (the "real fix" path) or remove the unprotected clause/criterion from the issue's scope and surface that to the user. Do NOT ship a requirement claim with no gate behind it; the workflow's contract is "every clause is verified by something durable," not "the carve-out lets us skip verification."
- **Re-validate the carve-out against the actual diff at the end of implementation.** The carve-out is checked against the *planned* diff at Step 4 and the *actual* diff at Step 4.5 (clause-by-clause verification) and again at Step 6 (completion gate). The Step 6 re-validation is a two-check sweep: (a) every changed path must be in the documentation set (`*.md`, ADRs, notes, docs, CHANGELOG, `changelog.d/**`, README, skills prose), and (b) every diff hunk's *content* must be free of executable behavior (no embedded code, no schema/grammar/policy data consumed by a runtime parser, no runnable fixtures). The path check alone is not enough — a doc file can carry executable behavior. If either check fails for any clause, the carve-out is invalidated retroactively for that clause; revert to the mandatory red-green loop for the executable portion AND for any clause whose structural gate was only a "no executable behavior" claim. The plan-time declaration is provisional; the actual diff is what counts.

If the carve-out applies, jump to Step 4.5; the loop below does not apply.

For all other diffs, the loop is mandatory:

1. **Write the failing test first.** For each clause of each in-scope requirement AND each acceptance criterion in the issue body, write a unit test that exercises the new behavior. Run the test and confirm it fails for the right reason (missing code, not a typo / wiring issue). A test you never saw fail is not a test — it's a guess.
2. **Write the minimum production code to make the test pass.** No premature abstraction, no scope creep, no "while I'm here" cleanups. Just enough to flip the failing assertion green.
3. **Refactor with the test green.** Clean up duplication, extract helpers, rename for clarity — but only with the safety net of green tests. Re-run the test after each refactor.
4. **Repeat per clause / acceptance criterion / edge case.** Do not write a batch of production code first and then "fill in tests" afterwards — that is not TDD, it is post-hoc test-shaped coverage and fails to drive the design.
5. **Edge cases and failure modes get tests too.** Validation errors, boundary inputs, conflict states, not-found paths, status transitions. If a behavior matters enough to ship, it matters enough for a red-green cycle.
6. **Integration / framework-specific test layers**: same loop. Write the failing test before the production code that satisfies it. Repository-policy rules from `cfg.rules.plan_rules_content` (e.g., framework-specific test requirements, migration policies) are TDD targets, not afterthoughts.
7. **If you discover during TDD that the plan is wrong**, stop and revise. The tests are telling you something — listen to them. Update the plan and post the revision as a follow-up comment on the issue.

Pre-existing tests around touched code must stay green at every step. If a refactor breaks an unrelated test, fix the root cause; do not silence the test.

### Step 4.5: Clause-by-Clause Verification

Before declaring implementation complete, build a mapping from every clause of every in-scope requirement AND every acceptance criterion in the issue body to the specific code (`file:line`) that satisfies it.

1. For each requirement in `in_scope_requirements[]`:
   - Re-read the requirement statement cached in Step 1.
   - Break it into individual clauses.
   - For EACH clause, identify the specific code (`file:line`) that satisfies it.
2. For each acceptance criterion stated in the issue body (or in the issue comments by the user):
   - Identify the specific code (`file:line`) that satisfies it.
3. If `in_scope_requirements[]` is empty AND the issue body has no explicit acceptance criteria, treat the issue title and description as the acceptance contract and verify the change matches.
4. If any clause or criterion is not satisfied, go back and implement it before proceeding.

Present the mapping as a checklist with the requirement UID (or `issue`) as the source label. Use the repo's source/test path conventions:

```
- [ ] GC-X004 clause: "..." → Satisfied by: {cfg.example_paths.source|default <repo source path>}/.../File.java:line
- [ ] GC-X004 clause: "..." → Satisfied by: {cfg.example_paths.test|default <repo test path>}/.../FileTest.java:line
- [ ] GC-X005 clause: "..." → Satisfied by: <other-relevant-path>:line
- [ ] issue acceptance: "..." → Satisfied by: {cfg.docs.adr_dir|default architecture/adrs/}0XX-name.md:line
```

Do not proceed until every clause and criterion is checked off.

Traceability reconciliation (IMPLEMENTS / TESTS links) and the `DRAFT → ACTIVE` status transitions are intentionally NOT done here. They land in Steps 15–17 after CI and all reviews have passed, so Ground Control state never runs ahead of the actual code that ships.

---

## Phase B: Quality Gate

### Step 5: Quality Assurance

- run `pre-commit run --all-files` to ensure the codebase is in a healthy state.

### Step 6: Completion Gate

Implementation is NOT ready for commit until ALL of the following are verified:

1. **Completion gate passes** — run `cfg.workflow.completion_command`. If that field is null, fall back to `cfg.workflow.test_command`. If both are null, ask the user what the completion gate command should be for this repo (do not guess). Confirm the command exits successfully.
2. **Changelog fragment present (or legitimately absent)** — if the diff touches application source (backend/frontend/MCP source, or `tools/` outside `tools/policy/` and `tools/tests/`), `git diff --name-only` MUST contain a valid fragment under `changelog.d/<issue>.<type>.md` (or `changelog.d/+<slug>.<type>.md`), type ∈ `security`/`added`/`changed`/`deprecated`/`removed`/`fixed`. A direct `CHANGELOG.md` edit does NOT satisfy a source-changing diff — that branch would re-open the rebase-storm pathology the convention exists to prevent. Direct `CHANGELOG.md` edits are reserved for release-collation commits, which by definition touch only `CHANGELOG.md` and the fragments they consumed (no source). CI-only diffs (only `.github/workflows/`) and docs-only diffs (`docs/**`, `architecture/**`, `README.md`, `CONTRIBUTING.md`, `.gc/**`, `skills/**`, etc.) carry no source paths and require no signal. `make policy` and `.claude/hooks/verify-implementation.sh` both encode the same predicate; if either flags `changelog-signal-missing` / `changelog-fragment-invalid-name`, add or rename the fragment.
3. **Step 4.5 clause mapping was completed** — if you skipped it, go back and do it now.
4. **If the documentation-only carve-out from Step 4.4 was declared**, re-validate it against the *actual* diff right now. The check must cover both committed AND uncommitted/untracked changes, since Step 6 runs *before* the Step 7/8 stage-and-commit step:
   1. **Compute the full path set.** Take the union of `git diff --name-only <base-ref>...HEAD` (committed work), `git diff --name-only HEAD` (unstaged), `git diff --cached --name-only` (staged), and `git ls-files --others --exclude-standard` (untracked-but-not-ignored). Working-tree state is part of the diff at this point in the workflow.
   2. **Path check (necessary, not sufficient).** Confirm every path in the union is documentation. The documentation set is intentionally narrow: `*.md`, `architecture/adrs/**`, `architecture/notes/**`, `docs/**`, `CHANGELOG.md`, `changelog.d/**`, `README.md`, `skills/**/*.md`, and equivalent doc-only locations declared by the repo. Any path outside that set — `*.java`, `*.py`, `*.ts`, `*.tsx`, `*.js`, `*.kts`, `*.gradle`, `*.yaml`/`*.yml` (workflows, configs), `*.sh`, `*.sql`, `Dockerfile`, `Makefile`, `*.json` (policies, package manifests, lockfiles), etc. — invalidates the carve-out outright.
   3. **Content check (the path check is not enough — a doc file can carry executable behavior).** For each path that survived check (2), inspect the actual diff content with `git diff <base-ref> -- <path>` (which against the working tree covers both committed and uncommitted changes). If any hunk introduces executable behavior — code fences whose contents are intended to be executed by tooling, embedded YAML that a code path parses and acts on, schema/grammar/policy data consumed by a runtime parser, runnable test fixtures, or any other line of static text whose meaning is "what the program should do at runtime" — the carve-out is invalidated for those clauses, and the mandatory red-green loop applies (write the failing test against the parser/consumer, then make the doc edit pass it).
   4. The carve-out passes Step 6 only when BOTH checks pass: every path is in the documentation set AND no diff hunk introduces executable behavior. If either check fails, revert to the mandatory red-green loop for the failing portion before declaring the gate passed.

If any check fails, fix it before proceeding. Do NOT move to Phase C until every check passes.

### Step 6.5: Pre-push Codex Review (final)

Run `gc_codex_review` with `uncommitted=true` against the staged + unstaged changes BEFORE the first push. **This is THE codex review pass for the PR** — there is no second post-push codex review (see issue #804). Merge-commit drift relative to the target branch is the responsibility of CI (compile/tests/integration) and SonarCloud (quality), not a separate codex pass.

Each codex review cycle takes ~5 min locally; each CI cycle to discover the same findings takes 10–15 min. Iterating locally collapses N CI runs into N local runs and one push.

1. Stage everything you intend to push (`git add -A` for the full diff).
2. Call the `gc_codex_review` MCP tool with:
   - `repo_path`: absolute path from Step 1
   - `base_branch`: `{cfg.workflow.base_branch|default dev}`
   - `uncommitted`: `true`
   - `issue_number`: the issue number resolved at Step 1, so the MCP server anchors the cycle counter to the issue thread (per ADR-029). When omitted, the server derives it from the current branch's leading numeric prefix (e.g. `796-cap-pre-push` → 796); pass it explicitly when the branch was named manually.
3. Apply the **Review loop rules** (defined below) to the returned findings, fix locally, re-stage, and re-invoke `gc_codex_review` until clean.
4. **Hard cap: 3 iterations** (enforced by the MCP server, issues #796 and #804). The next invocation reads existing markers on the issue thread, counts pre-push cycles **per issue** (the current branch is recorded in the marker for audit context but is NOT part of the cap key — a branch rename on the same issue cannot reset the counter), and refuses cycle 4 with `error: "codex_review_prepush_cap_reached"` and `next_action: "post_summary_and_escalate_to_user"`. After cycle 3, if findings remain, STOP, post the remaining findings + your fix history as an issue comment, and escalate to the user. If the user authorizes cycle 4, retry with `override_cap=true` and `override_reason="<one-line quote of the user's authorization>"`.
5. **Findings record**: every successful cycle posts a verbatim findings comment to the resolved issue thread (per ADR-029). The comment carries the cycle/cap/mode header and both reviewers' verbatim text. If that post fails, the run returns `ok: false, error: "review_comment_post_failed"` — fix the underlying GitHub issue (network, gh auth, repo perms) and retry.
6. Once clean, proceed to Phase C with the staged diff.

Skip this step only if the diff is so trivial (one-liner typo fix) that codex would have nothing to find. When in doubt, run it.

---

## Phase C: Stage, Commit, Push

### Step 7: Stage & Pre-commit Loop

1. `git add` all relevant changed files. Do NOT stage .env files, credentials, secrets, or large binaries.
2. Run `pre-commit run --all-files`.
3. If pre-commit fails:
   - Read the failure output.
   - Fix the issues.
   - Re-stage any modified files with `git add`.
   - Re-run `pre-commit run --all-files`.
   - Repeat up to 5 times. If still failing after 5 attempts, escalate to the user with the failure details (post as an issue comment).
4. When pre-commit passes, proceed.

### Step 8: Commit & Push

1. Craft a concise commit message in imperative mood (per coding standards). Example: "Add risk scoring engine for requirement prioritization"
2. NEVER include Co-Authored-By, "Generated with Claude Code", or any agent attribution in commit messages.
3. `git commit -m "<message>"`
4. `git push -u origin <branch>`

---

## Phase D: Ship

### Step 9: Create PR

1. Check if a PR already exists for this branch: `gh pr list --head <branch> --json number,url`
2. If no PR exists, create one:
   ```
   gh pr create --base {cfg.workflow.base_branch|default dev} --title "<concise title>" --body "<description with requirement reference>"
   ```
   The PR body should end with `Closes #<issue-number>` so the issue and PR are cross-linked in GitHub's UI.
3. Note the PR number and URL.

### Step 10: CI Monitor

`gh run watch` blocks indefinitely if no runner picks up the job. Use a bounded poll instead so the workflow surfaces stuck-queued conditions instead of hanging silently.

1. Find the latest workflow run: `gh run list --branch <branch> --limit 1 --json status,conclusion,databaseId,createdAt`. Cache the `databaseId` as `<id>` and the `createdAt` timestamp.
2. **Poll** `gh run view <id> --json status,conclusion` every 15 seconds. Track wall-clock elapsed time since you started polling.
3. **Queued-too-long guard.** If `status` is still `"queued"` after **5 minutes** of polling, STOP and report to the user that no runner accepted the job. For self-hosted runner pools, suggest checking the runner pool (`gh api /repos/<owner>/<repo>/actions/runners`) and confirming a runner is `online` and `idle`. Do NOT wait silently past this point.
4. **In-progress cap.** If `status` is `"in_progress"`, keep polling. Total wall-clock cap including the queued window is **45 minutes**. If the run has not reached `"completed"` by then, STOP and surface the run URL to the user.
5. When `status` becomes `"completed"`:
   - If `conclusion` is `"success"`, proceed.
   - Otherwise, get failed logs: `gh run view <id> --log-failed`. Diagnose, fix, `git add`, `git commit`, `git push`, and go back to step 1 of this phase.

### Step 11: SonarCloud

**Skip this step entirely if `cfg.sonarcloud` is null.** Log "SonarCloud skipped — no sonarcloud block in .ground-control.yaml" and proceed to Step 13.

This step runs AFTER Step 10 (CI Monitor) reports green. A green CI run does not imply a clean SonarCloud — the quality gate and the issue list are separate from CI conclusions and must be checked independently.

Otherwise:
1. Wait 60 seconds for SonarCloud analysis to propagate after the CI run.
2. Use `get_project_quality_gate_status` with `cfg.sonarcloud.project_key` to check the quality gate status for the current pull request.
3. **Pull the full open-issues list for the current PR using `$SONAR_TOKEN`.** The MCP `search_sonar_issues_in_projects` surface is the preferred interface; if it is unavailable or returns partial results, fall back to the REST API:

   ```
   curl -sS -u "$SONAR_TOKEN:" \
     "https://sonarcloud.io/api/issues/search?componentKeys=<project_key>&pullRequest=<PR_NUMBER>&resolved=false&ps=500"
   ```

   - `$SONAR_TOKEN` is provided via the environment. Never hardcode, echo, or commit it.
   - Request every page until all issues are retrieved (`ps=500` plus `p=2,3,...` until `total` is covered). Do not truncate.
   - Repeat the same query with `types=SECURITY_HOTSPOT` via `api/hotspots/search?projectKey=...&pullRequest=...&status=TO_REVIEW` so security hotspots are not missed.

4. **Fix every open issue the query returns — code-smell, bug, vulnerability, and security hotspot, every severity from INFO to BLOCKER, pre-existing or not.** If you think a finding is dangerous to fix, unwise in context, or a false positive, STOP, post your reasoning as an issue comment with `decision: <fix|wontfix|not-applicable>` and the rationale, and ask the user. Wait for their answer; do not push commits while the question is open.

5. For each fix cycle:
   - Apply the fixes.
   - Re-run the local completion gate to confirm nothing regressed locally.
   - `git add`, `git commit` with message `Fix SonarCloud findings (cycle <N>)`, `git push`.
   - Re-run Step 10 (CI Monitor) so SonarCloud re-analyzes the PR.
   - After CI is green, wait 60 seconds and re-run this entire step.

6. **Cycle cap: 5 iterations for SonarCloud.** If the issue list is still non-empty after 5 fix→re-analyze cycles, STOP, post the remaining findings as an issue comment, and escalate to the user.

7. Proceed to Step 13 only when: the quality gate is `OK` AND `api/issues/search?resolved=false` returns zero rows for this PR AND `api/hotspots/search?status=TO_REVIEW` returns zero rows for this PR.

## Review loop rules (apply to every review step in this skill)

Codex review now runs as a single pre-push pass at Step 6.5; the remaining review step in Phase D is **test quality review (Step 13)**. Both follow the **same loop**:

1. **Invoke the review.**
2. **Read the FULL output.** Do not stop after the first few findings.
3. **Classify each finding before touching anything: `one-off` or `class`.** Codex review supplies `classification` (and, for `class` findings, `category = {shape, instances}`) on each finding object — read it. If a finding arrived without a classification (e.g. test-quality review, which does not yet emit it), classify it yourself first.
   - **`one-off`** — this exact site, no analogues. Apply the named fix to the named site. This is the existing path.
   - **`class`** — this site is one instance of a recurring pattern (the same brittle construction, the same missing pre-condition, the same bypassed helper). **STOP. Do not apply the named fix to the named site yet.** Instead:
     1. Re-read the category's `shape` — what makes a site an instance? What pre-condition fails? What invariant is violated?
     2. Sweep the diff **and adjacent repo code where the category plausibly extends** for every instance — the ones codex listed in `category.instances` *and* any it missed.
     3. Design the fix to address the **category**, not the symptom: a structural gate, a shared helper, a parameterization, a single point of repair, an API change. The fix should be one place, not N.
     4. Apply that single design to every instance at once.
     5. Only then re-run the review.

     A `class` finding that you fixed only on the codex-named site is a process violation in the same shape as silent deferral — it leaves the category un-addressed, and the next review cycle surfaces another instance, burning a cycle the cap is not meant to absorb. If a category genuinely spans 5+ files outside the current feature's scope, that is the architectural-change escalation point — STOP, post the category + the affected files as an issue comment, and ask the user.
4. **Fix every finding, pre-existing or not.** The zero-deferral rule applies: there is no `defer` decision — not "out of scope for this PR", not "follow-up issue to track it", not "addressed in a subsequent PR", not "deferred to a later iteration", not "TBD later" in a closing comment. Filing a tracking issue does **not** convert a deferral into a valid disposition. The PreToolUse hook (`.claude/hooks/block-defer-language.py`) and `bin/policy` enforce this mechanically; the contract is fix-or-escalate. If you think a finding is dangerous to fix, unwise in context, or a false positive, STOP, post your reasoning as an issue comment with `decision: <fix|wontfix|not-applicable>` and rationale, and ask the user. Wait for their answer; do not push commits while the question is open. `wontfix` requires explicit user approval; `not-applicable` is for findings that don't actually apply (false positive on this codebase, finding outside the diff's scope, etc.).
5. **Record decisions on the issue thread.** Per ADR-029, every finding decision (`fix` / `wontfix` / `not-applicable`) gets a one-line rationale comment on the GitHub issue — a `class` finding's decision says how the category was closed, not just that the named site was patched. Agent silence on a finding is a process violation; text scanning catches *written* deferral language, but the issue-thread findings-vs-decisions reconciliation is what catches *silent* omission.
6. **Re-run the SAME review after fixing.** Do not assume your fixes are complete — the re-run is the verification.
7. **Repeat until the reviewer reports zero findings, OR the cycle cap is hit.**

For every cycle, after applying fixes the agent must update the tree the reviewer sees BEFORE re-running:

- **Step 6.5 (pre-push codex review)** is local-only. Re-stage with `git add -A` and re-invoke; do NOT commit or push between cycles. The pre-push review reads the staged + unstaged diff against the base branch.
- **Step 13 (test quality review)** runs after the PR is open. Commit and push fixes BEFORE re-invoking so the reviewer sees the updated tree.

Format every fix commit (Step 13 only) as `Fix review findings (<reviewer>, cycle <N>)` so the loop history is visible in git log.

<!-- Step 12 (post-push codex review) intentionally removed by issue #804.
     The single codex review pass now lives at Step 6.5 (pre-push). The
     tool-layer post-push entrypoint (`gc_codex_review` with a `pr_number`)
     remains as defense-in-depth for direct callers, but the SKILL no
     longer drives it as a workflow step. See ADR-029 (Pre-push review
     cycle state) for the rationale and the `gc_codex_verify_finding`
     per-finding cap that still applies when an agent does invoke the
     tool-layer post-push entrypoint by hand. -->

### Step 13: Test Quality Review

1. Invoke the `review-tests` skill at `skills/review-tests/SKILL.md`. Claude Code drivers call it via the `Skill` tool with `skill="review-tests"`. Codex drivers invoke it via `~/.codex/prompts/review-tests.md` (installed by `bin/install-skills.sh`). The same canonical content drives both — no driver-specific divergence.
2. Apply the **Review loop rules**: fix every finding, pre-existing or not, including "warning" level. Re-invoke after each fix cycle.
3. **Cycle cap: 5 iterations.** After the fifth, escalate to the user.

### Step 14: Final CI re-verification

After Step 13 (test quality review) has reported zero findings (or you have user-approved exceptions documented as decision comments on the issue):

1. Verify the branch is pushed with the latest fix commits.
2. Re-run Step 10 (CI Monitor) to confirm CI is still green.
3. Re-run Step 11 (SonarCloud) — or skip again if `cfg.sonarcloud` is null.
4. If either re-check fails, loop back through the appropriate review step — the cycle cap applies per review step, not total.

### Step 15: Transition In-Scope Requirements to ACTIVE

The status transition MUST happen BEFORE traceability reconciliation (Step 16). The Ground Control API enforces `IMPLEMENTS → ACTIVE`: any `gc_create_traceability_link` call with `link_type: IMPLEMENTS` against a `DRAFT` requirement returns `422 requirement_not_active`. Reconciling first therefore produces silent failures.

Semantically, moving a requirement from DRAFT to ACTIVE is the point at which the team commits to its statement. Once real code exists pointing at it, the requirement is no longer a proposal — it's a contract.

For each UID in `in_scope_requirements[]`:
- **First, classify the requirement against the actual diff:**
  - **Materially implemented (case in-diff)** — the diff itself contains the artifacts-of-record that satisfy the requirement's clauses (production code, tests, schema/migration files, configuration files, ADRs, workflow definitions, skill prose, or any other deliverable the requirement statement specifies).
  - **Materially implemented (case pre-existing)** — the diff finalizes/documents the requirement (e.g., an ADR clarification, changelog fragment, or workflow note marking the requirement complete) while the structural implementation already exists in pre-existing files shipped under a sibling requirement. The test is "does this PR ship the requirement," not "is the implementing code in this diff." For this case, **before transitioning the requirement**, use the discovery procedure below to identify the pre-existing artifact(s) of record. If discovery finds zero implementing files, the case-pre-existing classification is wrong: STOP, surface to the user (the requirement is either forward-looking, has missing implementation, or was misidentified), and do NOT transition.
  - **Forward-looking** — the diff documents or references the requirement but does not ship it (e.g., a schema field that an unimplemented future feature will consume, or a design note that anticipates work in a later wave). Use a `DOCUMENTS` link in Step 16 instead of `IMPLEMENTS`, and leave the status DRAFT. Surface this decision as a comment on the issue. Skip the rest of this loop for that UID.

- **Pre-existing artifact discovery procedure (case pre-existing only).** This MUST run before the ACTIVE transition for the case-pre-existing path, so Ground Control never gets promoted-without-coverage:
  1. Read the requirement statement and identify the named subsystems, file roots, modules, or component identifiers it references (e.g., "the Identity Center bootstrap module," "the state-boundary verifier," "the ingest pipeline's retry helper").
  2. Run `git ls-files` filtered to those roots, plus `git grep -l` (NOT `grep -r`) against the requirement's distinctive identifiers (UID, named module, distinctive function names) bounded to the subject-area paths. Use `git`-aware tools so the candidate set only contains tracked files — `grep -r` would also walk untracked / generated / `.gitignore`'d / build / `node_modules` paths and produce candidates for files that were never shipped, which would create traceability links to non-shipped artifacts. Do NOT scan the whole repo.
  3. **Validate each candidate file against the requirement statement** by reading it and confirming the file actually satisfies the clause(s) you mapped it to. The candidate list from grep/ls-files is a superset; the agent's read of file content against the requirement is what proves satisfaction. Discard candidates that do not actually satisfy the requirement.
  4. **For each surviving candidate**, classify it by intended link type and call `gc_get_traceability_by_artifact` to learn what it is already linked to (dedupe / preservation, NOT validation):
     - Production code, configuration files, ADR/design docs, workflow files → IMPLEMENTS link, with `artifact_type: CODE_FILE` / `CONFIG` / `ADR` / `DOCUMENTATION` as appropriate.
     - Automated tests that verify the requirement → TESTS link, with `artifact_type: TEST`.
     - Existing links to the same requirement remain valid; do not churn them. Existing links to a different requirement that still satisfy that requirement also remain valid; do not delete them.
  5. Cache the resulting candidate set as the *backfill targets*, partitioned by intended link type (IMPLEMENTS targets vs TESTS targets) — Step 16 Mode A reuses this partitioned set rather than re-discovering, and never creates an IMPLEMENTS link onto a candidate classified as a TEST.
  6. If the IMPLEMENTS partition is empty after a bounded, validated search, the case-pre-existing classification fails (see above).

- **Only after classification (and, for case pre-existing, after discovery succeeded)**, transition the materially-implemented requirements:
  - Use `gc_transition_status` to transition the requirement from `DRAFT` to `ACTIVE`.
  - If the requirement was already `ACTIVE`, skip it.
  - If the requirement was in any other state (`DEPRECATED`, `ARCHIVED`), STOP and surface the anomaly to the user — transitioning out of those states is a user decision.

If `in_scope_requirements[]` is empty, this step is a no-op. Proceed to Step 16 anyway — reconciliation still needs to run to catch drift on other requirements whose files this diff touched.

### Step 16: Reconcile Traceability Links Against the Diff

Now that CI and all reviews are green AND every materially-implemented in-scope requirement is ACTIVE, reconcile the Ground Control traceability graph against the actual diff. This MUST happen AFTER Step 15 and BEFORE Step 19.

**No deferral, here or anywhere downstream (ADR-029).** Reconciliation, the final report (Step 19), and any issue comment you post from here on must not contain deferral-disposition language — "out of scope for this PR", "follow-up issue to track X", "addressed in a subsequent PR", "deferred to a later iteration", "TBD later". A finding or a piece of work is either fixed in this PR, recorded `wontfix` with explicit user authorization, or recorded `not-applicable` with rationale. If reconciliation surfaces missing implementation for a requirement, that is a STOP-and-escalate, not a "noted as a follow-up". The `.claude/hooks/block-defer-language.py` PreToolUse hook will block a `gh issue comment` / `gh pr edit` carrying that language; do not work around it — re-route to fix-or-escalate.

**Reconciliation is not the same as "create links for the in-scope requirements"**. Even runs with zero in-scope requirements (pure bug fixes, refactors, maintenance) must reconcile, because the diff may have touched files that were already linked to OTHER requirements.

1. **Compute the touched file set.** Run `git diff --name-status <base-ref>...HEAD`. Cache the full list.

   Resolve `<base-ref>` using the configured base branch (`{cfg.workflow.base_branch|default dev}`) in this order:
   1. `origin/{cfg.workflow.base_branch|default dev}` (verify with `git rev-parse --verify`).
   2. `{cfg.workflow.base_branch|default dev}` (local, verify with `git rev-parse --verify`).
   3. `origin/main` (fallback for repos that don't use `dev`).
   4. `main` (fallback).
   5. If none resolve, run `git fetch origin {cfg.workflow.base_branch|default dev}` and retry. If the fetch fails (no network/no remote), STOP and surface a clear error.

2. **Process deleted and renamed files first.**
   For every deleted file `path`:
   - Call `gc_get_traceability_by_artifact` with `artifact_type: CODE_FILE` and `artifact_identifier: path`. Repeat with `artifact_type: TEST`.
   - For each link found: if behavior moved to a new file (rename or split), delete the stale link and create a replacement at the new path. If behavior was removed entirely and the linked requirement no longer has any implementation, STOP — ripping out the only implementation of a requirement is a user decision.
   For every renamed file `old_path → new_path`:
   - Delete the stale link and re-create it at `new_path`.

3. **Process modified files.**
   For every modified file `path`:
   - Call `gc_get_traceability_by_artifact` with `artifact_type: CODE_FILE` (or `TEST` for test files).
   - For each existing link, decide: still satisfies the linked requirement? → leave alone. Behavior moved? → delete stale link, create at new location. Behavior now spans more files? → add additional links.
   - Inspect for behaviors that satisfy under-linked requirements and create new links. Bound by plausible subject area; don't compare every requirement to every file.

4. **Process added files.**
   For every added file `path`:
   - Determine which requirement(s) it satisfies. Create IMPLEMENTS (production) or TESTS (test files) links via `gc_create_traceability_link`. Incidental files (helpers, fixtures, generated) may have no link — that's fine.

5. **Ensure every in-scope requirement has coverage appropriate to its nature.**

   **Mode A — the diff ships the work.** For each UID in `in_scope_requirements[]`:
   - **IMPLEMENTS coverage is required against the artifact(s) of record.** Every materially-implemented in-scope requirement must have at least one IMPLEMENTS link pointing at the file(s) that actually satisfy its clauses. Those files may be in the diff (Step 15 case in-diff), pre-date the diff (Step 15 case pre-existing — finalization/documentation runs whose structural implementation lives in a sibling requirement's earlier code), or both. The shape of "implementation" depends on the requirement: code requirements → production file; documentation requirements → ADR/SCHEMA/docs file; configuration requirements → config file; workflow requirements → workflow file / hook script. When the diff itself adds documentation that defines the requirement's contract (e.g., an ADR section), link that ADR with IMPLEMENTS too; pure-housekeeping diff entries (e.g., changelog fragments under `changelog.d/`) need no link.
   - **Backfill onto pre-existing artifacts when the diff finalizes the requirement.** If Step 15 classified the requirement as case pre-existing, the *backfill targets* are already cached from Step 15's discovery procedure, partitioned by intended link type. Create IMPLEMENTS links pointing at each candidate in the IMPLEMENTS partition; create TESTS links pointing at each candidate in the TESTS partition. Use `gc_create_traceability_link` with the partition's link type. Never create an IMPLEMENTS link onto a candidate classified as a TEST. Apply the *Backfill rules* below. Do not invent a "diff-only" IMPLEMENTS link onto an ADR or changelog fragment as a substitute for linking the actual implementing code.
   - **TESTS coverage is conditional.** Add a TESTS link when the diff introduces or touches an automated test that verifies the requirement, OR when discovery's TESTS partition (Step 15) contains a pre-existing automated test that verifies it. TESTS is NOT required for documentation / configuration / structural-invariant requirements with no executable behavior. IMPLEMENTS alone is the complete coverage record in those cases.
   - **Do not fabricate test links.** If a requirement has testable behavior and no test was added or discovered, go back to Step 4.4.
   - **Never link the diff to a requirement it does not satisfy** just to satisfy this step. Surface the mismatch to the user instead.
   - **Forward-looking requirements** (DRAFT requirements this PR does not materially implement) get DOCUMENTS links, not IMPLEMENTS, and stay DRAFT.

   **Backfill rules (apply to Mode A case pre-existing and to Mode B below).**
   - Reuse the discovery procedure documented in Step 15 (subject-area-bounded `git ls-files` / `grep` against named subsystems/file roots, then `gc_get_traceability_by_artifact` per candidate). Do NOT compare every requirement to every file in the repo.
   - Existing links to files that still satisfy the requirement remain valid. Do not churn links merely because the current PR touched a nearby document.
   - If backfill discovers no implementing file anywhere in the repo, STOP — either the requirement should be demoted (DEPRECATED) or implementation is missing. Surface to the user. (For Mode A case pre-existing this should be impossible — Step 15 would have refused the transition first.)

   **Mode B — Step 4 concluded the work is already complete.** The diff is empty (Step 4 step 5 path); forcing an IMPLEMENTS link onto a non-existent diff file would be wrong. Instead:
   - **Accept existing IMPLEMENTS coverage.** If the requirement already has IMPLEMENTS links pointing at files that exist and still satisfy it, that coverage is complete. Do NOT fabricate new links.
   - **Backfill only when nothing is linked.** If the requirement has zero existing IMPLEMENTS links, locate the implementing file(s) per the *Backfill rules* above and create the link(s).
   - **TESTS rules from Mode A still apply.**

6. **Reconcile the issue → requirement links (both directions).**
   - **Add missing links.** For each UID in `in_scope_requirements[]`, ensure there is a `GITHUB_ISSUE` link with `artifact_identifier: <issue-number>` on the requirement. Use `IMPLEMENTS` for materially-implemented requirements (matches `gc_create_github_issue`'s auto-link convention), `DOCUMENTS` for forward-looking ones. **Never** use `TESTS` — an issue is not an executable test. Material implementation status of the *code* is captured by separate `IMPLEMENTS` links from the requirement to actual code/ADR/config files (Step 5 above).
   - **Delete stale links.** Call `gc_get_traceability_by_artifact` with `artifact_type: GITHUB_ISSUE` and `artifact_identifier: <issue-number>`. For each returned link, if the requirement UID is NOT in `in_scope_requirements[]`, delete it.
   - Duplicate-create is intentionally rejected by `gc_create_traceability_link`, so re-adding is safe.
   - Note: `gc_create_github_issue` auto-creates `IMPLEMENTS` issue→requirement links during UID-first runs (Step 6). That is correct for materially-implemented requirements. For a forward-looking in-scope requirement that this PR does not deliver, manually delete the auto-`IMPLEMENTS` link and replace with a `DOCUMENTS` link before this reconciliation step exits.

Reconciliation is idempotent: running it on an already-correct branch is a no-op.

### Step 17: Verify Ground Control State Landed

1. For each UID in `in_scope_requirements[]`:
   - Re-fetch with `gc_get_requirement` and confirm status is `ACTIVE` for materially-implemented requirements (DRAFT for forward-looking ones, with DOCUMENTS links instead of IMPLEMENTS).
   - Re-fetch with `gc_get_traceability` and confirm the expected links are present.
2. Re-run the deleted/renamed/modified audit from Step 16: every file in the diff should either have up-to-date links or be intentionally un-linked.
3. If anything is missing or drifted, loop back to fix.
4. **Never declare success on silent failures.** If any `gc_create_traceability_link` / `gc_delete_traceability_link` / `gc_transition_status` call returned non-2xx, treat as failure, surface to the user, loop back.

### Step 18: Close the Issue

Close the GitHub issue now via `gh issue close <issue-number>`, then clear the in-progress flag set in Step 1: `gh issue edit <issue-number> --remove-label in-progress`. The work is done, the PR records it, GitHub's auto-close on merge is unreliable. If the label removal fails, surface it in the Step 19 report rather than claiming the issue was closed cleanly.

### Step 19: Report (DO NOT MERGE)

**You MUST NOT merge the PR. You MUST NOT run `gh pr merge`. The user reviews and merges.**

Provide a final summary as a comment on the issue thread (the durable record per ADR-029):
- Issue number and title
- `in_scope_requirements[]` — each UID + title, with its new status (ACTIVE for implemented, DRAFT for forward-looking)
- Plan-comment URL (cached in Step 4)
- Files created, modified, renamed, or deleted
- Traceability reconciliation summary — links added, deleted, updated; which requirements gained coverage
- Review findings and decisions (codex / refactor / test-quality / SonarCloud) — with their issue-comment links
- Confirmation: CI green, SonarCloud passed (or skipped), PR ready for user review and merge
- PR URL
