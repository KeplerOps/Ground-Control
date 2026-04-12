#!/usr/bin/env bash
#
# bootstrap-skills.sh — make ~/.claude/skills/<name> point at the skills checked
# into this repo (.claude/skills/<name>).
#
# This repo is the source of truth for the Claude skills we iterate on. The
# Claude Code runtime reads from ~/.claude/skills/, so each host needs the
# user-level entries to resolve into the checked-in copies. Run this once per
# host after cloning, or after any host-level reset that wipes ~/.claude/skills.
#
# Behavior:
#   - For each directory under .claude/skills/ in this repo, create (or
#     replace) a symlink at ~/.claude/skills/<name> pointing at it.
#   - If a pre-existing ~/.claude/skills/<name> is a regular directory with
#     local modifications that are NOT in the repo, the script refuses to
#     clobber it and exits non-zero. Pass --force to overwrite anyway — only
#     do this if you know the repo copy is the version you want.
#   - Skills that only exist in ~/.claude/skills/ (not in the repo) are left
#     alone.
#
# Usage:
#   scripts/bootstrap-skills.sh [--force] [--dry-run]
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SKILLS_SRC="$REPO_ROOT/.claude/skills"
SKILLS_DST="${HOME}/.claude/skills"

force=0
dry_run=0
for arg in "$@"; do
  case "$arg" in
    --force) force=1 ;;
    --dry-run) dry_run=1 ;;
    -h|--help)
      sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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

if [[ ! -d "$SKILLS_SRC" ]]; then
  echo "error: $SKILLS_SRC does not exist — run from inside a Ground-Control checkout" >&2
  exit 1
fi

mkdir -p "$SKILLS_DST"

linked=0
skipped=0
clobbered=0

shopt -s nullglob
for skill_dir in "$SKILLS_SRC"/*/; do
  name="$(basename "$skill_dir")"
  src="$SKILLS_SRC/$name"
  dst="$SKILLS_DST/$name"

  if [[ -L "$dst" ]]; then
    # Already a symlink — check where it points.
    current="$(readlink "$dst")"
    if [[ "$current" == "$src" ]]; then
      skipped=$((skipped + 1))
      continue
    fi
    echo "updating symlink $dst: $current -> $src"
    run "rm -f \"$dst\" && ln -s \"$src\" \"$dst\""
    linked=$((linked + 1))
    continue
  fi

  if [[ -e "$dst" ]]; then
    # Real directory / file in the way. Only overwrite when the user opts in
    # AND the contents already match the repo (so we don't silently delete
    # unsaved work).
    if [[ -d "$dst" ]] && diff -qr "$src" "$dst" > /dev/null 2>&1; then
      echo "replacing identical directory $dst with symlink -> $src"
      run "rm -rf \"$dst\" && ln -s \"$src\" \"$dst\""
      clobbered=$((clobbered + 1))
      continue
    fi
    if [[ "$force" -eq 1 ]]; then
      echo "FORCE: replacing $dst with symlink -> $src"
      run "rm -rf \"$dst\" && ln -s \"$src\" \"$dst\""
      clobbered=$((clobbered + 1))
      continue
    fi
    echo "refusing to replace $dst (differs from repo copy). Re-run with --force to overwrite." >&2
    exit 3
  fi

  echo "linking $dst -> $src"
  run "ln -s \"$src\" \"$dst\""
  linked=$((linked + 1))
done
shopt -u nullglob

echo
echo "bootstrap-skills: linked=$linked clobbered=$clobbered already-correct=$skipped"
