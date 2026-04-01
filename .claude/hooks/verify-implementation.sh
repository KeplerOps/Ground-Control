#!/usr/bin/env bash
# Stop hook: blocks Claude from finishing if implementation checklist is incomplete.
# Only runs when on a GC requirement branch with source code changes.

set -euo pipefail

INPUT=$(cat)

# Don't loop — if we're already in a stop-hook retry, let it through
STOP_HOOK_ACTIVE=$(echo "$INPUT" | jq -r '.stop_hook_active // false')
if [ "$STOP_HOOK_ACTIVE" = "true" ]; then
  exit 0
fi

cd /home/atomik/src/Ground-Control

# Only check if there are changes against dev
CHANGED=$(git diff --name-only origin/dev...HEAD 2>/dev/null || echo "")
if [ -z "$CHANGED" ]; then
  exit 0
fi

REASONS=""

# Check 1: CHANGELOG.md must be in the diff if source files changed
HAS_CHANGELOG=$(echo "$CHANGED" | grep -c 'CHANGELOG.md' || true)
if [ "$HAS_CHANGELOG" -eq 0 ]; then
  REASONS="${REASONS}CHANGELOG.md was not updated despite source code changes. "
fi

# Check 2: docs/API.md should be updated if new endpoints were added
HAS_CONTROLLER=$(echo "$CHANGED" | grep -c 'Controller\.java' || true)
HAS_API_DOC=$(echo "$CHANGED" | grep -c 'docs/API.md' || true)
if [ "$HAS_CONTROLLER" -gt 0 ] && [ "$HAS_API_DOC" -eq 0 ]; then
  REASONS="${REASONS}New controller added but docs/API.md not updated. "
fi

# Check 3: MCP parity — if controllers changed, lib.js and index.js should too
HAS_LIB=$(echo "$CHANGED" | grep -c 'lib\.js' || true)
HAS_INDEX=$(echo "$CHANGED" | grep -c 'index\.js' || true)
if [ "$HAS_CONTROLLER" -gt 0 ] && { [ "$HAS_LIB" -eq 0 ] || [ "$HAS_INDEX" -eq 0 ]; }; then
  REASONS="${REASONS}New controller added but MCP tools not updated (lib.js/index.js). "
fi

# Check 4: /review and /security-review must have been invoked
BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
SKILL_LOG="/tmp/claude-skill-log/${BRANCH}.jsonl"

if [ -f "$SKILL_LOG" ]; then
  HAS_REVIEW=$(grep -c '"skill":"review"' "$SKILL_LOG" || true)
  HAS_SEC_REVIEW=$(grep -c '"skill":"security-review"' "$SKILL_LOG" || true)
else
  HAS_REVIEW=0
  HAS_SEC_REVIEW=0
fi

if [ "$HAS_REVIEW" -eq 0 ]; then
  REASONS="${REASONS}Code review (/review) was not invoked. You MUST call Skill(review) before completing. "
fi
if [ "$HAS_SEC_REVIEW" -eq 0 ]; then
  REASONS="${REASONS}Security review (/security-review) was not invoked. You MUST call Skill(security-review) before completing. "
fi

if [ -n "$REASONS" ]; then
  jq -n --arg reason "$REASONS" '{"decision": "block", "reason": $reason}'
  exit 0
fi

# All checks passed
exit 0
