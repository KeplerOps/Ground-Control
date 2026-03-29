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

# Check 1: CHANGELOG.md must be in the diff if source files changed
REASONS=""
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

if [ -n "$REASONS" ]; then
  jq -n --arg reason "$REASONS" '{"decision": "block", "reason": $reason}'
  exit 0
fi

# All checks passed
exit 0
