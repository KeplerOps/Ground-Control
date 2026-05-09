#!/usr/bin/env bash
#
# install-skills.sh
#
# Installs the canonical Ground Control skills (under skills/) into the
# Claude Code skill directory and the Codex prompts directory on this host.
#
# Defaults to symlinks so the agent always reads the latest source-of-truth
# from this repo. Use --copy if symlinks aren't viable (e.g., Windows without
# developer-mode symlinks). Idempotent: re-run after pulling to refresh.
#
# Usage:
#   bin/install-skills.sh [--copy] [--dry-run] [--no-codex] [--claude-dir <path>] [--codex-dir <path>]
#
# Options:
#   --copy            Hard-copy each skill instead of symlinking.
#   --dry-run         Print actions without writing anything.
#   --no-codex        Skip the Codex install target (use when Codex isn't on this host or its convention isn't settled).
#   --claude-dir P    Override the Claude Code install root (default: ~/.claude/skills).
#   --codex-dir P     Override the Codex install root (default: ~/.codex/prompts).
#
# Per ADR-027: the canonical workflow lives at skills/<name>/SKILL.md in this
# repo. Host-local files are install targets, not the source of truth.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
skills_root="${repo_root}/skills"

mode="symlink"
dry_run=0
install_codex=1
claude_dir="${HOME}/.claude/skills"
codex_dir="${HOME}/.codex/prompts"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --copy)
      mode="copy"; shift ;;
    --dry-run)
      dry_run=1; shift ;;
    --no-codex)
      install_codex=0; shift ;;
    --claude-dir)
      claude_dir="$2"; shift 2 ;;
    --codex-dir)
      codex_dir="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,/^$/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2 ;;
  esac
done

if [[ ! -d "${skills_root}" ]]; then
  echo "ERROR: ${skills_root} does not exist. Are you running this from a Ground Control checkout?" >&2
  exit 1
fi

run() {
  if [[ "${dry_run}" -eq 1 ]]; then
    printf 'DRY-RUN: %s\n' "$*"
  else
    "$@"
  fi
}

# Install Claude Code skills: each subdir of skills/ becomes ~/.claude/skills/<name>
run mkdir -p "${claude_dir}"
for skill_dir in "${skills_root}"/*/; do
  [[ -d "${skill_dir}" ]] || continue
  name="$(basename "${skill_dir}")"
  target="${claude_dir}/${name}"

  # Replace existing target whether it's a file, directory, or symlink.
  if [[ -e "${target}" || -L "${target}" ]]; then
    run rm -rf -- "${target}"
  fi

  if [[ "${mode}" == "symlink" ]]; then
    run ln -sfn "${skill_dir%/}" "${target}"
  else
    run cp -R "${skill_dir%/}" "${target}"
  fi
  printf '%-7s claude   %s -> %s\n' "${mode}" "${target}" "${skill_dir%/}"
done

# Install Codex prompts: each skills/<name>/SKILL.md becomes ~/.codex/prompts/<name>.md
# The Codex prompt convention is still settling; if --no-codex is set we skip
# this entirely. Repos can opt out per-host by passing --no-codex.
if [[ "${install_codex}" -eq 1 ]]; then
  run mkdir -p "${codex_dir}"
  for skill_dir in "${skills_root}"/*/; do
    [[ -d "${skill_dir}" ]] || continue
    name="$(basename "${skill_dir}")"
    src="${skill_dir%/}/SKILL.md"
    [[ -f "${src}" ]] || continue
    target="${codex_dir}/${name}.md"

    if [[ -e "${target}" || -L "${target}" ]]; then
      run rm -f -- "${target}"
    fi

    if [[ "${mode}" == "symlink" ]]; then
      run ln -sfn "${src}" "${target}"
    else
      run cp -- "${src}" "${target}"
    fi
    printf '%-7s codex    %s -> %s\n' "${mode}" "${target}" "${src}"
  done
else
  echo "Skipping Codex install (--no-codex set)."
fi

echo "Done."
