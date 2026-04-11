#!/bin/bash
set -euo pipefail

REPO="KeplerOps/Ground-Control"
WORKFLOW="pack-registry-sync.yml"
REF="$(git rev-parse --abbrev-ref HEAD)"
PROJECT="ground-control"
PACK_IDS=""

usage() {
    cat <<EOF
Usage: $0 [options]

Options:
  --project <id>      Ground Control project identifier (default: ground-control)
  --pack-ids <csv>    Optional comma-separated pack IDs to sync
  --ref <branch>      Git ref to dispatch from (default: current branch)
  --repo <owner/name> Override GitHub repository (default: KeplerOps/Ground-Control)
  --help              Show this help text

Examples:
  $0
  $0 --pack-ids nist-sp800-53-rev5,nist-sp800-171-rev3
  $0 --ref main --project ground-control
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project)
            PROJECT="${2:?missing value for --project}"
            shift 2
            ;;
        --pack-ids)
            PACK_IDS="${2:?missing value for --pack-ids}"
            shift 2
            ;;
        --ref)
            REF="${2:?missing value for --ref}"
            shift 2
            ;;
        --repo)
            REPO="${2:?missing value for --repo}"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if ! command -v gh >/dev/null 2>&1; then
    echo "gh CLI is required." >&2
    exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
    echo "gh CLI is not authenticated." >&2
    exit 1
fi

if ! gh api "repos/${REPO}/actions/workflows/${WORKFLOW}" >/dev/null 2>&1; then
    cat >&2 <<EOF
Workflow ${WORKFLOW} is not registered in ${REPO}.

GitHub only exposes workflow_dispatch workflows after the workflow file exists on the repository's default branch.
Push or merge the workflow to the default branch, then rerun this script.
EOF
    exit 1
fi

echo "Dispatching ${WORKFLOW}"
echo "Repository: ${REPO}"
echo "Ref: ${REF}"
echo "Project: ${PROJECT}"
if [[ -n "${PACK_IDS}" ]]; then
    echo "Pack IDs: ${PACK_IDS}"
fi

cmd=(
    gh workflow run "${WORKFLOW}"
    --repo "${REPO}"
    --ref "${REF}"
    -f "project=${PROJECT}"
)

if [[ -n "${PACK_IDS}" ]]; then
    cmd+=(-f "pack_ids=${PACK_IDS}")
fi

"${cmd[@]}"

echo "Workflow triggered. View runs at: https://github.com/${REPO}/actions/workflows/${WORKFLOW}"
