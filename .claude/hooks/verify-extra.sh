#!/usr/bin/env bash
# Project-specific implementation checks for Ground-Control.
# Sourced by the user-level verify-implementation.sh Stop hook.
# $CHANGED is passed in as an env var containing the git diff file list.
# Output any failure reasons to stdout; empty output = all checks pass.

ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

if ! OUTPUT=$(cd "$ROOT" && python3 bin/policy --files-env CHANGED --skip-pr-body 2>&1); then
  echo -n "$OUTPUT"
fi
