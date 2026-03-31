#!/bin/bash
# Ground Control database restore — from local dump or S3
set -euo pipefail

set -a; source /opt/gc/.env; set +a

BACKUP_DIR=/data/backups
COMPOSE_FILE=/opt/gc/docker-compose.yml
BUCKET="${BACKUP_BUCKET:-groundcontrol-backups-catalyst-dev}"
SKIP_CONFIRM="${SKIP_CONFIRM:-false}"

usage() {
  echo "Usage: restore.sh [--list | --from-file <path> | --from-s3 <key>] [--yes]"
  echo ""
  echo "Options:"
  echo "  --list            List available backups (local and S3)"
  echo "  --from-file PATH  Restore from a local pg_dump file"
  echo "  --from-s3 KEY     Download from S3 and restore (e.g. pg-dumps/gc-20260329-030000.dump)"
  echo "  --yes             Skip confirmation prompt"
}

list_backups() {
  echo "=== Local backups ==="
  ls -lht "${BACKUP_DIR}"/gc-*.dump 2>/dev/null || echo "  (none)"
  echo ""
  echo "=== S3 backups ==="
  aws s3 ls "s3://${BUCKET}/pg-dumps/" 2>/dev/null || echo "  (none)"
}

verify_restore() {
  echo "Verifying database health..."
  docker compose -f "${COMPOSE_FILE}" exec -T db \
    pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null
  TABLE_COUNT=$(docker compose -f "${COMPOSE_FILE}" exec -T db \
    psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -A \
    -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")
  echo "Verified: ${TABLE_COUNT} public tables present."
}

do_restore() {
  local DUMP_FILE="$1"
  [ -f "${DUMP_FILE}" ] || { echo "ERROR: File not found: ${DUMP_FILE}"; exit 1; }

  echo "Creating pre-restore safety backup..."
  /opt/gc/backup.sh

  if [ "${SKIP_CONFIRM}" != "true" ]; then
    read -p "Restore from ${DUMP_FILE}? This will REPLACE the current database. [y/N] " -r
    [[ $REPLY =~ ^[Yy]$ ]] || { echo "Aborted."; exit 1; }
  fi

  echo "Restoring from ${DUMP_FILE}..."
  docker compose -f "${COMPOSE_FILE}" exec -T db \
    pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists < "${DUMP_FILE}"

  verify_restore
  echo "Restore complete."
}

# Parse arguments
MODE=""
FILE_ARG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --list) MODE="list"; shift ;;
    --from-file) MODE="file"; FILE_ARG="$2"; shift 2 ;;
    --from-s3) MODE="s3"; FILE_ARG="$2"; shift 2 ;;
    --yes) SKIP_CONFIRM="true"; shift ;;
    *) usage; exit 1 ;;
  esac
done

case "${MODE}" in
  list) list_backups ;;
  file) do_restore "${FILE_ARG}" ;;
  s3)
    LOCAL_PATH="${BACKUP_DIR}/$(basename "${FILE_ARG}")"
    echo "Downloading s3://${BUCKET}/${FILE_ARG}..."
    aws s3 cp "s3://${BUCKET}/${FILE_ARG}" "${LOCAL_PATH}"
    do_restore "${LOCAL_PATH}"
    ;;
  *) usage; exit 1 ;;
esac
