---
name: ship
description: Monitor CI, check SonarCloud, run code and security reviews, fix all issues, and prepare for merge. Run after committing and pushing.
disable-model-invocation: true
---

# Ship Current Branch

The user handles committing and pushing (GPG signing required).
Assume the user has already committed and pushed before invoking this skill.

## Phase 1: CI Monitor

1. Determine the current branch: `git branch --show-current`
2. Find the latest workflow run: `gh run list --branch <branch> --limit 1 --json status,conclusion,databaseId`
3. If the run is in progress, watch it: `gh run watch <id>`
4. If it failed, get failed logs: `gh run view <id> --log-failed`
5. Diagnose the failure and fix the issue.
6. If fixes were made, tell the user: "Fixes applied. Please commit and push, then tell me to continue."
7. Stop and wait for the user. When they confirm, go back to step 2.

## Phase 2: SonarCloud Check

Once CI is green:
1. Wait 60 seconds for SonarCloud analysis to propagate.
2. Use `get_project_quality_gate_status` with project key `KeplerOps_Ground-Control` to check the quality gate.
3. Use `search_sonar_issues_in_projects` to find new issues on the current branch.
4. Fix any issues found.
5. If fixes were made, tell the user to commit and push, then re-run Phase 1.

## Phase 3: Code Review

1. Merge dev into the current branch: `git fetch origin dev && git merge origin/dev`
2. If there are merge conflicts, resolve them and tell the user to commit and push.
3. Review all changes against the PR's requirement and coding standards.
4. Use the /pr-review approach: check coding standards, review quality, verify requirement coverage.
5. Fix ALL issues. Do NOT defer any issues.
6. If fixes were made, note them for the user.

## Phase 4: Security Review

1. Use the /security-review approach on the changes.
2. Fix ALL issues that are fixable without introducing significant risk.
3. If fixes were made, note them for the user.

## Phase 5: Final

1. If ANY fixes were made in phases 3-4, tell the user to commit and push.
2. After user confirms push, re-run Phase 1 (CI monitor) one final time.
3. Once everything is green, report:
   - Summary of all changes made during the ship process
   - Confirmation that CI is green
   - Confirmation that SonarCloud passed
   - The branch is ready to merge
