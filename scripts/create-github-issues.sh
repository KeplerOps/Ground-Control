#!/usr/bin/env bash
set -euo pipefail

# Create GitHub issues from .github/issues/**/*.md files.
# Includes rate-limit delay between API calls.
#
# Usage:
#   ./scripts/create-github-issues.sh              # dry-run (default)
#   ./scripts/create-github-issues.sh --execute    # actually create issues
#   ./scripts/create-github-issues.sh --phase 0    # only phase 0 issues

DELAY=4  # seconds between API calls (GitHub allows ~30 req/min for mutations)
DRY_RUN=true
PHASE_FILTER=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --execute) DRY_RUN=false; shift ;;
    --phase) PHASE_FILTER="$2"; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

ISSUE_DIR=".github/issues"
CREATED=0
SKIPPED=0
FAILED=0

if [[ ! -d "$ISSUE_DIR" ]]; then
  echo "Error: $ISSUE_DIR not found. Run from repo root." >&2
  exit 1
fi

# --- Ensure all labels exist ---
echo "Checking labels..."

# Collect all labels used across issue files
declare -A ALL_LABELS
while IFS= read -r line; do
  raw=$(echo "$line" | sed 's/^labels: *\[//;s/\]$//')
  IFS=',' read -ra parts <<< "$raw"
  for l in "${parts[@]}"; do
    l=$(echo "$l" | xargs)
    [[ -n "$l" ]] && ALL_LABELS["$l"]=1
  done
done < <(grep -rh '^labels:' "$ISSUE_DIR")

# Add phase-N and priority labels
while IFS= read -r p; do
  p=$(echo "$p" | xargs)
  [[ -n "$p" ]] && ALL_LABELS["phase-$p"]=1
done < <(grep -rh '^phase:' "$ISSUE_DIR" | sed 's/^phase: *//')

while IFS= read -r p; do
  p=$(echo "$p" | xargs)
  [[ -n "$p" ]] && ALL_LABELS["$p"]=1
done < <(grep -rh '^priority:' "$ISSUE_DIR" | sed 's/^priority: *//')

# Label colors by category
label_color() {
  local name="$1"
  case "$name" in
    phase-*)    echo "0e8a16" ;;  # green
    P0)         echo "b60205" ;;  # red
    P1)         echo "d93f0b" ;;  # orange
    P2)         echo "fbca04" ;;  # yellow
    *)          echo "c5def5" ;;  # light blue
  esac
  return 0
}

# Fetch existing labels
mapfile -t EXISTING < <(gh label list --limit 200 --json name -q '.[].name')
declare -A EXISTING_SET
for el in "${EXISTING[@]}"; do
  EXISTING_SET["$el"]=1
done

LABELS_CREATED=0
for label in "${!ALL_LABELS[@]}"; do
  if [[ -z "${EXISTING_SET[$label]:-}" ]]; then
    color=$(label_color "$label")
    if [[ "$DRY_RUN" == false ]]; then
      if gh label create "$label" --color "$color" 2>/dev/null; then
        LABELS_CREATED=$((LABELS_CREATED + 1))
      else
        echo "  Warning: failed to create label '$label'"
      fi
      sleep 1
    else
      LABELS_CREATED=$((LABELS_CREATED + 1))
    fi
  fi
done
echo "Labels: $LABELS_CREATED new (${#ALL_LABELS[@]} total needed, ${#EXISTING[@]} already existed)"
echo "---"

# Collect and sort issue files
mapfile -t FILES < <(find "$ISSUE_DIR" -name '*.md' ! -name 'README.md' | sort)

echo "Found ${#FILES[@]} issue files"
if [[ "$DRY_RUN" == true ]]; then
  echo "DRY RUN — pass --execute to create issues"
fi
echo "---"

for f in "${FILES[@]}"; do
  # Parse frontmatter
  title=$(awk '/^title:/{gsub(/^title: *"?|"$/,""); print; exit}' "$f")
  labels_raw=$(awk '/^labels:/{gsub(/^labels: *\[|\]$/,""); print; exit}' "$f")
  phase=$(awk '/^phase:/{gsub(/^phase: */,""); print; exit}' "$f")
  priority=$(awk '/^priority:/{gsub(/^priority: */,""); print; exit}' "$f")

  # Filter by phase if requested
  if [[ -n "$PHASE_FILTER" && "$phase" != "$PHASE_FILTER" ]]; then
    continue
  fi

  # Build label list (strip spaces, add phase/priority labels)
  labels=""
  IFS=',' read -ra LABEL_ARR <<< "$labels_raw"
  for l in "${LABEL_ARR[@]}"; do
    l=$(echo "$l" | xargs)  # trim whitespace
    if [[ -n "$l" ]]; then
      labels="${labels:+$labels,}$l"
    fi
  done
  if [[ -n "$phase" ]]; then
    labels="${labels:+$labels,}phase-$phase"
  fi
  if [[ -n "$priority" ]]; then
    labels="${labels:+$labels,}$priority"
  fi

  # Extract body (everything after second ---)
  body=$(awk 'BEGIN{c=0} /^---$/{c++; next} c>=2{print}' "$f")

  if [[ -z "$title" ]]; then
    echo "SKIP (no title): $f"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  echo "[$((CREATED + SKIPPED + FAILED + 1))/${#FILES[@]}] $title"
  echo "  Labels: $labels"
  echo "  File: $f"

  if [[ "$DRY_RUN" == false ]]; then
    if gh issue create \
      --title "$title" \
      --label "$labels" \
      --body "$body" 2>/tmp/gh-issue-err; then
      CREATED=$((CREATED + 1))
      echo "  -> Created"
    else
      FAILED=$((FAILED + 1))
      echo "  -> FAILED: $(cat /tmp/gh-issue-err)"
    fi
    echo "  (waiting ${DELAY}s...)"
    sleep "$DELAY"
  else
    CREATED=$((CREATED + 1))
  fi

  echo ""
done

echo "---"
echo "Done. Created: $CREATED | Skipped: $SKIPPED | Failed: $FAILED"
if [[ "$DRY_RUN" == true ]]; then
  echo "(Dry run — no issues were actually created)"
fi
