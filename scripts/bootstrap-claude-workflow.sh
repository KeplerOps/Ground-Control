#!/usr/bin/env bash
#
# bootstrap-claude-workflow.sh — make ~/.claude/{skills,hooks}/<name> point at
# the copies checked into this repo, so this repo is the source of truth for
# the workflow surfaces Claude Code actually loads at runtime.
#
# What gets symlinked:
#
#   skills/ — every directory under .claude/skills/ in this repo becomes a
#   symlink at ~/.claude/skills/<name>. Full directory symlink so the runtime
#   picks up any sibling files the skill owner adds later (assets, prompts,
#   etc.), not just SKILL.md.
#
#   hooks/ — only the user-level workflow hooks listed in WORKFLOW_HOOKS
#   below. Repo-scoped hooks (protect_files.sh, verify-extra.sh) are wired
#   via $CLAUDE_PROJECT_DIR in .claude/settings.json and must NOT be linked
#   into ~/.claude/hooks/, so the allowlist is explicit.
#
# Pre-existing host state:
#
#   - If ~/.claude/<kind>/<name> is already the desired symlink, it's left
#     alone.
#   - If it's a plain file/directory whose content is byte-identical to the
#     repo copy, it's replaced with a symlink automatically.
#   - If it differs from the repo copy, the script refuses to touch it and
#     exits non-zero. Re-run with --force to clobber.
#   - Entries that exist user-level but not in the repo are never touched.
#
# Usage:
#   scripts/bootstrap-claude-workflow.sh [--force] [--dry-run]
#
# Run once per host after cloning Ground-Control. Safe to re-run.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SKILLS_SRC="$REPO_ROOT/.claude/skills"
SKILLS_DST="${HOME}/.claude/skills"
HOOKS_SRC="$REPO_ROOT/.claude/hooks"
HOOKS_DST="${HOME}/.claude/hooks"

# User-level hooks that live in this repo. Only these are symlinked.
# Repo-scoped hooks (protect_files.sh, verify-extra.sh) are NOT in this list
# because settings.json references them via $CLAUDE_PROJECT_DIR, not ~/.claude.
WORKFLOW_HOOKS=(
  "git-merge-guard.py"
  "log-skill-call.sh"
  "verify-implementation.sh"
)

force=0
dry_run=0
for arg in "$@"; do
  case "$arg" in
    --force) force=1 ;;
    --dry-run) dry_run=1 ;;
    -h|--help)
      sed -n '2,33p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

run() {
  if [[ "$dry_run" -eq 1 ]]; then
    echo "DRY-RUN: $*"
  else
    eval "$@"
  fi
}

linked=0
clobbered=0
already_correct=0

# Link a single source -> destination, with the refuse-unless-identical-or-force
# safety rule. $1 source path, $2 destination path, $3 "dir"|"file".
link_one() {
  local src="$1"
  local dst="$2"
  local kind="$3"

  if [[ -L "$dst" ]]; then
    local current
    current="$(readlink "$dst")"
    if [[ "$current" == "$src" ]]; then
      already_correct=$((already_correct + 1))
      return 0
    fi
    echo "updating symlink $dst: $current -> $src"
    run "rm -f \"$dst\" && ln -s \"$src\" \"$dst\""
    linked=$((linked + 1))
    return 0
  fi

  if [[ -e "$dst" ]]; then
    local identical=0
    if [[ "$kind" == "dir" ]]; then
      [[ -d "$dst" ]] && diff -qr "$src" "$dst" > /dev/null 2>&1 && identical=1
    else
      [[ -f "$dst" ]] && diff -q "$src" "$dst" > /dev/null 2>&1 && identical=1
    fi

    if [[ "$identical" -eq 1 ]]; then
      echo "replacing identical $kind $dst with symlink -> $src"
      run "rm -rf \"$dst\" && ln -s \"$src\" \"$dst\""
      clobbered=$((clobbered + 1))
      return 0
    fi
    if [[ "$force" -eq 1 ]]; then
      echo "FORCE: replacing $dst with symlink -> $src"
      run "rm -rf \"$dst\" && ln -s \"$src\" \"$dst\""
      clobbered=$((clobbered + 1))
      return 0
    fi
    echo "refusing to replace $dst (differs from repo copy). Re-run with --force to overwrite." >&2
    exit 3
  fi

  echo "linking $dst -> $src"
  run "ln -s \"$src\" \"$dst\""
  linked=$((linked + 1))
}

# --- skills ---
if [[ ! -d "$SKILLS_SRC" ]]; then
  echo "error: $SKILLS_SRC does not exist — run from inside a Ground-Control checkout" >&2
  exit 1
fi
mkdir -p "$SKILLS_DST"

shopt -s nullglob
for skill_dir in "$SKILLS_SRC"/*/; do
  name="$(basename "$skill_dir")"
  link_one "$SKILLS_SRC/$name" "$SKILLS_DST/$name" "dir"
done
shopt -u nullglob

# --- workflow hooks ---
if [[ ! -d "$HOOKS_SRC" ]]; then
  echo "error: $HOOKS_SRC does not exist — run from inside a Ground-Control checkout" >&2
  exit 1
fi
mkdir -p "$HOOKS_DST"

for hook_name in "${WORKFLOW_HOOKS[@]}"; do
  src="$HOOKS_SRC/$hook_name"
  if [[ ! -f "$src" ]]; then
    echo "warning: $src is declared in WORKFLOW_HOOKS but missing from the repo" >&2
    continue
  fi
  link_one "$src" "$HOOKS_DST/$hook_name" "file"
done

echo
echo "bootstrap-claude-workflow: linked=$linked clobbered=$clobbered already-correct=$already_correct"
