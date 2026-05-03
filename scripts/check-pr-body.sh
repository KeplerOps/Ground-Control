#!/usr/bin/env bash
#
# Pre-push policy check: if the current branch has an open pull request,
# fetch its body and validate it against the Ground Control template
# requirements (`bin/policy --pr-body-file`). Catches missing sections
# before the push triggers a CI run that would fail at the policy job.
#
# Skips silently when:
#   - no branch is checked out (detached HEAD), or
#   - no PR exists for the current branch yet (first push of a branch is
#     fine — the body is validated again on PR creation), or
#   - `gh` is not on PATH (developer hasn't installed the GitHub CLI).
#
# To bypass deliberately, run `git push --no-verify`.

set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  exit 0
fi

branch="$(git symbolic-ref --short -q HEAD || true)"
if [ -z "${branch}" ]; then
  exit 0
fi

# `gh pr view --head <branch>` is broken in older versions, but `gh pr list`
# is reliable. Look for an open PR with this branch as the head ref.
pr_number="$(gh pr list --head "${branch}" --state open --json number --jq '.[0].number // empty' 2>/dev/null || true)"
if [ -z "${pr_number}" ]; then
  exit 0
fi

tmp_body="$(mktemp -t gc-pr-body.XXXXXX)"
trap 'rm -f "${tmp_body}"' EXIT
gh pr view "${pr_number}" --json body --jq '.body' > "${tmp_body}"

repo_root="$(git rev-parse --show-toplevel)"
exec python3 "${repo_root}/bin/policy" --pr-body-file "${tmp_body}"
