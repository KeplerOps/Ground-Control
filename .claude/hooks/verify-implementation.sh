#!/usr/bin/env bash
# Stop hook: blocks Claude from finishing if implementation checklist is incomplete.
# Only enforces when /implement was invoked in THIS session (scoped by $PPID).
#
# Universal check: a source-changing diff MUST carry a valid changelog
# fragment under `changelog.d/<issue>.<type>.md` (or `+<slug>.<type>.md`)
# per the towncrier-style convention adopted in issue #848 (ADR-021 Phase
# B). Direct `CHANGELOG.md` edits do NOT satisfy a source-changing diff —
# accepting them would re-open the rebase-storm pathology this change
# exists to prevent. Direct edits are reserved for release-collation
# commits, which touch only `CHANGELOG.md` and the fragments they
# consumed (no application source) and therefore fall through the source
# predicate. CI-only and docs-only diffs likewise carry no source paths
# and need no signal. There is no "pure refactor" carve-out — refactors
# under application-source paths still file a fragment. The vocabulary
# and source predicate mirror `tools/policy/checks.py`; the
# `test_hook_regex_matches_policy_vocabulary` and
# `test_hook_gates_on_application_source_predicate` tests keep the
# two layers in sync.
#
# /review and /security-review are intentionally NOT enforced here — /implement
# owns its own review gate via gc_codex_review and /review-tests. See
# .claude/skills/implement/SKILL.md.

set -euo pipefail

INPUT=$(cat)

# Don't loop — if we're already in a stop-hook retry, let it through
STOP_HOOK_ACTIVE=$(echo "$INPUT" | jq -r '.stop_hook_active // false')
if [ "$STOP_HOOK_ACTIVE" = "true" ]; then
  exit 0
fi

# Only enforce if /implement was invoked in this session
SKILL_LOG="/tmp/claude-skill-log/${PPID}.jsonl"
if [ ! -f "$SKILL_LOG" ]; then
  exit 0
fi

HAS_IMPLEMENT=$(grep -c '"skill":"implement"' "$SKILL_LOG" || true)
if [ "$HAS_IMPLEMENT" -eq 0 ]; then
  exit 0
fi

# Find repo root (no hardcoded paths)
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
if [ -z "$REPO_ROOT" ]; then
  exit 0
fi
cd "$REPO_ROOT"

# Determine base branch (dev if it exists, otherwise main)
if git rev-parse --verify origin/dev >/dev/null 2>&1; then
  BASE="origin/dev"
else
  BASE="origin/main"
fi

# Build the full path set the policy claims to enforce: committed work
# against the base branch, unstaged working-tree changes, staged-but-
# uncommitted changes, and untracked-but-not-ignored paths. Step 6 runs
# before `git commit`, so a working-tree edit that isn't yet committed
# still has to satisfy the gate. The `--diff-filter=ACDMRTUXB` filter
# matches the policy-check side and explicitly includes deletions (D),
# since deleting application source is itself a change that needs a
# release-notes signal.
CHANGED=$( {
  git diff --name-only --diff-filter=ACDMRTUXB "${BASE}...HEAD" 2>/dev/null || true
  git diff --name-only --diff-filter=ACDMRTUXB HEAD 2>/dev/null || true
  git diff --cached --name-only --diff-filter=ACDMRTUXB 2>/dev/null || true
  git ls-files --others --exclude-standard 2>/dev/null || true
} | sort -u )
if [ -z "$CHANGED" ]; then
  exit 0
fi

REASONS=""

# Check 1: source-changing diff must carry a valid changelog fragment.
# Mirrors `tools/policy/checks.py::run_changelog_fragment_check`:
#   - Application-source paths: `backend/src/main/`, `backend/src/test/`,
#     `frontend/src/`, `mcp/`, or `tools/` (excluding `tools/policy/` and
#     `tools/tests/`).
#   - Required signal: a fragment named `changelog.d/<issue>.<type>.md` or
#     `changelog.d/+<slug>.<type>.md` where <type> is one of the six
#     Keep-a-Changelog categories. Direct `CHANGELOG.md` edits do NOT
#     satisfy source diffs (release-collation commits have no source
#     changes, so they fall through the predicate).
# Vocabulary and source-path predicate MUST match the Python check —
# kept in sync by `test_hook_regex_matches_policy_vocabulary` and
# `test_hook_gates_on_application_source_predicate`.
#
# Each count pipeline below is pipefail-safe: `grep -c ... || true` masks
# the no-match exit status, and the `awk` form returns 0 naturally. A
# bare `grep -E ... | wc -l` chain would abort the hook under
# `set -euo pipefail` on docs-only / CI-only diffs.
HAS_SOURCE=$(echo "$CHANGED" \
  | grep -E -c '^(backend/src/main/|backend/src/test/|frontend/src/|mcp/)' \
  || true)
HAS_TOOLS_SOURCE=$(echo "$CHANGED" \
  | awk '/^tools\// && !/^tools\/(policy|tests)\//' \
  | grep -c '.' || true)
TOTAL_SOURCE=$((HAS_SOURCE + HAS_TOOLS_SOURCE))
if [ "$TOTAL_SOURCE" -gt 0 ]; then
  # Each candidate fragment path must EXIST in the working tree to count
  # as a signal — a fragment listed in `$CHANGED` may be a deletion (the
  # `D` slot of `--diff-filter=ACDMRTUXB`). A release-collation commit
  # deletes the fragments it consumed; without this existence check,
  # those deletions would silently count as signals for an unrelated
  # source change in the same PR.
  HAS_FRAGMENT=0
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    if [ -f "$REPO_ROOT/$f" ]; then
      HAS_FRAGMENT=1
      break
    fi
  done <<<"$(echo "$CHANGED" \
    | grep -E '^changelog\.d/([0-9]+|\+[A-Za-z0-9][A-Za-z0-9._-]*)\.(security|added|changed|deprecated|removed|fixed)\.md$' \
    || true)"
  if [ "$HAS_FRAGMENT" -eq 0 ]; then
    REASONS="${REASONS}Source-changing diff has no valid changelog fragment under changelog.d/. Add a fragment named <issue>.<type>.md (or +<slug>.<type>.md), type in {security,added,changed,deprecated,removed,fixed}. Direct CHANGELOG.md edits are reserved for release collation. See changelog.d/README.md. "
  fi
fi

# Run project-specific checks if they exist
EXTRA_CHECKS="$REPO_ROOT/.claude/hooks/verify-extra.sh"
if [ -f "$EXTRA_CHECKS" ]; then
  EXTRA_REASONS=$(CHANGED="$CHANGED" bash "$EXTRA_CHECKS" || true)
  if [ -n "$EXTRA_REASONS" ]; then
    REASONS="${REASONS}${EXTRA_REASONS}"
  fi
fi

if [ -n "$REASONS" ]; then
  jq -n --arg reason "$REASONS" '{"decision": "block", "reason": $reason}'
  exit 0
fi

# All checks passed
exit 0
