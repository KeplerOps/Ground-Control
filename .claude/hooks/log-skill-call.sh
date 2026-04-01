#!/usr/bin/env bash
# PostToolUse hook: logs Skill tool invocations to a session log file.
# The Stop hook (verify-implementation.sh) checks this log to confirm
# that /review and /security-review were actually invoked.

INPUT=$(cat)
SKILL_NAME=$(echo "$INPUT" | jq -r '.tool_input.skill // empty')

if [ -z "$SKILL_NAME" ]; then
  exit 0
fi

LOG_DIR="/tmp/claude-skill-log"
mkdir -p "$LOG_DIR"

# Use git branch as session identifier so each feature branch has its own log
BRANCH=$(git -C /home/atomik/src/Ground-Control branch --show-current 2>/dev/null || echo "unknown")
LOG_FILE="$LOG_DIR/${BRANCH}.jsonl"

echo "{\"skill\":\"$SKILL_NAME\",\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" >> "$LOG_FILE"
exit 0
