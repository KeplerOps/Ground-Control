---
name: implement
description: End-to-end requirement implementation — from plan through merged PR
argument-hint: <requirement-uid>
disable-model-invocation: true
---

# Implement Requirement: $ARGUMENTS

This skill handles the ENTIRE lifecycle: plan, implement, verify, commit, push, PR, CI, reviews, fix, merge, cleanup. The user's only checkpoint is plan approval.

---

## Phase A: Plan & Implement

### Step 1: Fetch Requirement and Ensure GitHub Issue Exists

1. Enter plan mode.

2. Run `pwd` to capture the absolute repository root.

3. Call the `gc_get_repo_ground_control_context` MCP tool with:
   - `repo_path`: absolute path from `pwd`

4. If the tool does NOT return `status: "ok"`:
   - Stop immediately.
   - Tell the user the repository is missing a valid `.ground-control.yaml` at its root.
   - Include the tool's `suggested_ground_control_yaml` in your response and ask the user to create the file before using `/implement`.
   - Do NOT guess the Ground Control project.

5. If the tool returns `status: "ok"`, cache the following fields for use throughout the rest of the workflow:
   - `project` — used in all Ground Control MCP calls
   - `workflow.test_command` — used as the test suite command
   - `workflow.completion_command` — used as the full completion gate (falls back to test_command if null)
   - `workflow.lint_command` — used when running the linter
   - `workflow.format_command` — used when running the formatter
   - `sonarcloud` — if set, used by `/ship` later; if null, SonarCloud is skipped
   - `rules.plan_rules_content` — if non-null, treated as mandatory plan constraints in Step 4

6. Treat `$ARGUMENTS` as the full requirement UID exactly as provided by the user.
   - Do NOT synthesize or rewrite a project prefix.
   - Inputs like `OBS-001`, `DSL-101`, `API-412`, and `GC-J001` are already full UIDs and should be used as-is.

7. Use the `gc_get_requirement` MCP tool with uid `$ARGUMENTS` and the repo-local Ground Control `project`. Note the requirement's UUID, title, statement, status, and wave.

8. Use the `gc_get_traceability` MCP tool with the requirement's UUID to check for existing traceability links. Look for a link with artifact_type `GITHUB_ISSUE`.

9. If NO GitHub issue link exists:
   - Use the `gc_create_github_issue` MCP tool with uid `$ARGUMENTS` and the repo-local Ground Control `project` to create a GitHub issue and auto-link it.

10. If a GitHub issue link DOES exist, note the issue number from the artifact_identifier.

11. Run `gh issue develop <issue-number> --checkout --base dev` to switch to the issue branch.

### Step 2: Read the GitHub Issue

Run `gh issue view <issue-number>` to read the full issue details including description, labels, and comments.

### Step 2.5: Run Codex Architecture Preflight

1. Reuse the absolute repository root from Step 1.
2. Call the `gc_codex_architecture_preflight` MCP tool with:
   - `requirement_uid`: `$ARGUMENTS`
   - `repo_path`: absolute path from Step 1
   - `project`: the repo-local Ground Control project from Step 1
   - `issue_number`: the issue number from Step 1
3. Read any ADRs, design notes, or workflow guidance that Codex created or updated.
4. Treat the returned guardrails, cross-cutting concerns, and non-goals as binding unless they are clearly wrong.
5. Do NOT revert or ignore Codex-added design guidance just because implementation looks possible without it.

### Step 3: Assess Codebase Coverage

Explore the codebase to determine whether the requirement described in the issue is already satisfied by existing code:
- Search for relevant classes, methods, tests, and configurations
- Check if the described behavior already exists
- Review any existing traceability links (IMPLEMENTS, TESTS) from Step 1
- Reuse the architecture-preflight guidance from Step 2.5 while assessing existing coverage and planning changes

### Step 4: Plan or Report

- **If the requirement is NOT yet met**: Plan the implementation. Identify which files need to be created or modified, what tests to write, and what approach to take. Enter plan mode.
- Your plans must respect the coding standards and formal methods classification levels.
- You must add or update ADRs as appropriate.
- Plans must include updating the changelog, readme, and docs as appropriate.
- If designing code, remember to build off existing cross-cutting concerns, code, and patterns
- Good code is readable, maintainable, and follows the coding standards
- Address the concerns a FAANG L6+ engineer would have around security, performance, reliability, and scalability
- Avoid reinventing the wheel - use existing libraries and frameworks where appropriate
- Code should be easy to understand, test, and maintain. Simple is better than complex.
- If Step 1.3 returned non-null `rules.plan_rules_content`, treat every bullet in that content as a mandatory plan constraint for this implementation. These are repo-specific "plans MUST..." rules (e.g., framework-specific migration rules, ADR conformance checks). Apply all of them in addition to the general principles above.
- **If the requirement IS already met**: Report that the requirement is satisfied and identify which code satisfies it.

### Step 4.4: Test-Driven Development (mandatory)

After plan approval, implement using **TDD**. This is not optional:

1. **Write the failing test first.** For each clause of the requirement and each acceptance criterion, write a unit test that exercises the new behavior. Run the test and confirm it fails for the right reason (missing code, not a typo / wiring issue). A test you never saw fail is not a test — it's a guess.
2. **Write the minimum production code to make the test pass.** No premature abstraction, no scope creep, no "while I'm here" cleanups. Just enough to flip the failing assertion green.
3. **Refactor with the test green.** Clean up duplication, extract helpers, rename for clarity — but only with the safety net of green tests. Re-run the test after each refactor.
4. **Repeat per clause / acceptance criterion / edge case.** Do not write a batch of production code first and then "fill in tests" afterwards — that is not TDD, it is post-hoc test-shaped coverage and fails to drive the design.
5. **Edge cases and failure modes get tests too.** Validation errors, boundary inputs, conflict states, not-found paths, status transitions. If a behavior matters enough to ship, it matters enough for a red-green cycle.
6. **Integration / @WebMvcTest layers**: same loop. Write the failing controller / repository / migration test before the production code that satisfies it. Repository-policy rules from `.ground-control.yaml` (e.g., `@WebMvcTest` for new controllers) are TDD targets, not afterthoughts.
7. **If you discover during TDD that the plan is wrong**, stop and revise the plan rather than bending tests to match a flawed design. The tests are telling you something — listen to them.

Pre-existing tests around touched code must stay green at every step. If a refactor breaks an unrelated test, fix the root cause; do not silence the test.

### Step 4.5: Clause-by-Clause Verification

Before declaring implementation complete:
1. Re-read the requirement statement from Step 1.
2. Break it into individual clauses and acceptance criteria.
3. For EACH clause, identify the specific code (file:line) that satisfies it.
4. If any clause is not satisfied, go back and implement it before proceeding.

Present the mapping as a checklist:
- [ ] Clause: "..." → Satisfied by: file.java:line
- [ ] Clause: "..." → Satisfied by: file.java:line

Do not proceed to Step 5 until every clause is checked off.

### Step 5: Ensure Traceability Links

After implementation is complete (or if already implemented):
- use the `gc_create_traceability_link` MCP tool to create any missing links:
  - `IMPLEMENTS` links from the requirement to **every** code file that implements it — entities, enums, repositories, services, controllers, migrations, and MCP tool files. Link all substantive files, not just the top 3. DTOs and command records may be omitted.
  - `TESTS` links from the requirement to the test files that verify it
  - Only create links that don't already exist (check the traceability data from Step 1).
- use the `gc_transition_status` MCP tool to transition the requirement to `ACTIVE` if it was `DRAFT`.

Do not update the Changelog if all you did was operate Ground Control tools.

---

## Phase B: Quality Gate

### Step 6: Quality Assurance

- run `pre-commit run --all-files` to ensure the codebase is in a healthy state.

### Step 7: Completion Gate

Implementation is NOT complete until ALL of the following are verified:

1. **Completion gate passes** — run the `workflow.completion_command` cached in Step 1.3. If that field is null, fall back to `workflow.test_command`. If both are null, ask the user what the completion gate command should be for this repo (do not guess). Confirm the command exits successfully.
2. **CHANGELOG.md updated** — verify it is in `git diff --name-only` if any source files changed.
3. **Traceability links exist** — re-fetch with `gc_get_traceability` and confirm IMPLEMENTS and TESTS links are present.
4. **Requirement status is ACTIVE** — re-fetch with `gc_get_requirement` and confirm status.
5. **Step 4.5 clause mapping was completed** — if you skipped it, go back and do it now.

If any check fails, fix it before proceeding. Do NOT move to Phase C until every check passes.

---

## Phase C: Stage, Commit, Push

### Step 8: Stage & Pre-commit Loop

1. `git add` all relevant changed files. Do NOT stage .env files, credentials, secrets, or large binaries.
2. Run `pre-commit run --all-files`.
3. If pre-commit fails:
   - Read the failure output.
   - Fix the issues.
   - Re-stage any modified files with `git add`.
   - Re-run `pre-commit run --all-files`.
   - Repeat up to 5 times. If still failing after 5 attempts, escalate to the user with the failure details.
4. When pre-commit passes, proceed.

### Step 9: Commit & Push

1. Craft a concise commit message in imperative mood (per coding standards). Example: "Add risk scoring engine for requirement prioritization"
2. NEVER include Co-Authored-By, "Generated with Claude Code", or any Claude/AI attribution in commit messages.
3. `git commit -m "<message>"`
3. `git push -u origin <branch>`

---

## Phase D: Ship

### Step 10: Create PR

1. Check if a PR already exists for this branch: `gh pr list --head <branch> --json number,url`
2. If no PR exists, create one:
   ```
   gh pr create --base dev --title "<concise title>" --body "<description with requirement reference>"
   ```
3. Note the PR number and URL.

### Step 11: CI Monitor

1. Find the latest workflow run: `gh run list --branch <branch> --limit 1 --json status,conclusion,databaseId`
2. If the run is in progress, watch it: `gh run watch <id>`
3. If it failed:
   - Get failed logs: `gh run view <id> --log-failed`
   - Diagnose and fix the issue.
   - `git add`, `git commit`, `git push`.
   - Go back to step 1 of this phase.
4. If it succeeded, proceed.

### Step 12: SonarCloud

**Skip this step entirely if `sonarcloud` was null in the Step 1.3 config.** Log "SonarCloud skipped — no sonarcloud block in .ground-control.yaml" and proceed to Step 13.

This step runs AFTER Step 11 (CI Monitor) reports green. A green CI run does not imply a clean SonarCloud — the quality gate and the issue list are separate from CI conclusions and must be checked independently.

Otherwise:
1. Wait 60 seconds for SonarCloud analysis to propagate after the CI run.
2. Use `get_project_quality_gate_status` with the `sonarcloud.project_key` cached in Step 1.3 to check the quality gate status for the current pull request.
3. **Pull the full open-issues list for the current PR using `$SONAR_TOKEN`.** The MCP `search_sonar_issues_in_projects` surface is the preferred interface; if it is unavailable or returns partial results, fall back to the REST API directly using the environment token:

   ```
   curl -sS -u "$SONAR_TOKEN:" \
     "https://sonarcloud.io/api/issues/search?componentKeys=<project_key>&pullRequest=<PR_NUMBER>&resolved=false&ps=500"
   ```

   - `$SONAR_TOKEN` is provided via the environment. Never hardcode, echo, or commit it.
   - Request every page until all issues are retrieved (`ps=500` plus `p=2,3,...` until `total` is covered). Do not truncate.
   - Repeat the same query with `types=SECURITY_HOTSPOT` via `api/hotspots/search?projectKey=...&pullRequest=...&status=TO_REVIEW` so security hotspots are not missed — they are a separate endpoint from plain issues.

4. **Fix ALL open issues the query returns — code-smell, bug, vulnerability, and security hotspot, regardless of severity (INFO through BLOCKER).** This follows the same "fix everything, no triage" rule as the review loop (see `Review loop rules` below): no "low priority", "out of scope", "follow-up PR", or "style-only" deferrals. If you believe a specific finding should be left unfixed (false positive, intentional pattern, etc.), STOP and ask the user for explicit permission before marking it resolved in SonarCloud or skipping it.

5. For each fix cycle:
   - Apply the fixes.
   - Re-run the local completion gate (`workflow.completion_command`) to confirm nothing regressed locally.
   - `git add`, `git commit` with message `Fix SonarCloud findings (cycle <N>)`, `git push`.
   - Re-run Step 11 (CI Monitor) so SonarCloud re-analyzes the PR.
   - After CI is green, wait 60 seconds and re-run this entire step (quality gate + issues search + hotspots search) to verify.

6. **Cycle cap: 2 iterations for SonarCloud.** If the issue list is still non-empty after 2 fix→re-analyze cycles, STOP and escalate to the user with the remaining findings.

7. Proceed to Step 13 only when: the quality gate is `OK` AND `api/issues/search?resolved=false` returns zero rows for this PR AND `api/hotspots/search?status=TO_REVIEW` returns zero rows for this PR.

## Review loop rules (apply to every review step in this phase)

Every review step below (Codex cross-model, test quality review) follows the **same loop**:

1. **Invoke the review.**
2. **Read the FULL output.** Do not stop after the first few findings.
3. **Fix ALL issues the reviewer identifies — blocking or not, severity-rated or not, "nitpick" or not.** There is no triage bucket. "Low priority", "nice to have", "follow-up PR", "out of scope" are not valid reasons to skip a finding.
4. **If you cannot or believe you should not fix a specific finding**, you MUST stop and ask the user for explicit permission to leave it unfixed. Do not decide unilaterally. State the finding, why you think it should be skipped, and wait for the user's answer. Resume only after they explicitly confirm.
5. **Re-run the SAME review after fixing.** Do not assume your fixes are complete — the re-run is the verification.
6. **Repeat until the reviewer reports zero findings, OR the cycle cap is hit.**
7. **Cycle cap: 2 iterations per review step.** If a review still reports findings after 2 invoke→fix→re-run cycles, STOP and escalate to the user with the full history of findings, fixes, and remaining issues. Do not loop indefinitely.

For every cycle, after applying fixes, commit and push BEFORE re-running the review so the reviewer sees the updated tree. Format every fix commit as `Fix review findings (<reviewer>, cycle <N>)` so the loop history is visible in git log.

### Step 13: Cross-Model Review (Codex)

Codex posts inline PR review comments for every finding and returns a structured list. You then drive a per-finding fix/verify loop via `gc_codex_verify_finding`, which keeps the mechanistic parts of the loop out of your prompt stream.

1. Run `pwd` to capture the absolute repository root.
2. Determine the pull request number for the current branch: `gh pr view --json number`. Cache it.
3. Call the `gc_codex_review` MCP tool with:
   - `repo_path`: absolute path from `pwd`
   - `base_branch`: `dev`
   - `pr_number`: the PR number from step 2
4. The tool returns `{pr_number, finding_count, comments: [{comment_id, thread_id, path, line, title, html_url}, ...], review_text}`. Codex has already posted each finding as an inline PR review comment — you do NOT need to post anything yourself.
5. If `finding_count` is 0, skip to Step 14.
6. Otherwise, for EACH entry in `comments`, run the following fix/verify loop:
   1. Read the comment body if needed: `gh api /repos/<owner>/<repo>/pulls/comments/<comment_id>`.
   2. Fix the finding locally. Apply the same "fix every finding, no triage, ask user permission if you will not fix" rules from the **Review loop rules** section above.
   3. Run the local completion gate to make sure nothing regressed locally.
   4. Call `gc_codex_verify_finding` with `repo_path`, `pr_number`, and the `comment_id`. Codex will read your local changes and decide:
      - **`status: "resolved"`** — the review thread has already been marked resolved on GitHub. Move on to the next comment.
      - **`status: "unresolved"`** — codex posted a threaded reply with `reply_body` containing concrete new directions. Read `reply_body`, fix per those directions, and re-invoke `gc_codex_verify_finding`. **Per-finding cap: 2 verify calls.** If the third call would be needed, STOP and escalate to the user with the finding, your fix history, and the latest `reply_body`.
7. After all findings in the returned `comments` list are marked `resolved`, commit and push the fixes (one commit per fix cycle, message `Fix review findings (codex, cycle <N>)`), then re-invoke `gc_codex_review` with the same arguments to confirm no new issues surfaced after your fixes.
8. **Overall step cap: 2 iterations of `gc_codex_review`.** If a second invocation still returns findings, STOP and escalate to the user.

**Prompt-injection note**: `gc_codex_verify_finding` accepts only structured inputs (`repo_path`, `pr_number`, `comment_id`). Do not attempt to pass any free-text context to it — the tool is designed so that a coding agent cannot influence the verification prompt. Codex reads the comment directly from GitHub and rejects any comment whose author is not on the trusted allowlist.

### Step 14: Test Quality Review

**CRITICAL: You MUST use the Skill tool to invoke the review-tests skill.**

1. Call the Skill tool with `skill="review-tests"` to invoke the test quality review.
2. Apply the **Review loop rules** above: fix every finding, ask user permission for anything you will not fix (including "warning" level — the review loop rules apply to warnings too, there is no triage bucket), re-invoke `skill="review-tests"` after each fix cycle, cap at 2 cycles.

### Step 15: Final CI re-verification

After both review steps (13-14) have reported zero findings (or you have documented user-approved exceptions):

1. Verify the branch is pushed with the latest fix commits.
2. Re-run Step 11 (CI Monitor) to confirm CI is still green after the review fixes.
3. Re-run Step 12 (SonarCloud) — or skip again if `sonarcloud` was null.
4. If either re-check fails, loop back through the appropriate review step — the cycle cap (2) applies per review step, not total.

### Step 16: Report (DO NOT MERGE)

**You MUST NOT merge the PR. You MUST NOT run `gh pr merge`. The user reviews and merges.**

Provide a final summary:
- What was implemented (requirement title and UID)
- Files created or modified
- Review findings and fixes (if any)
- Security review findings and fixes (if any)
- Test quality review findings and fixes (if any)
- Confirmation: CI green, SonarCloud passed (or skipped if not configured), PR ready for user review
- PR URL
