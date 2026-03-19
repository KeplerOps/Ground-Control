#!/bin/bash
# Ground Control database backup — pg_dump to local + S3
# Runs daily via cron at 03:00 UTC
set -euo pipefail

BACKUP_DIR=/data/backups
COMPOSE_FILE=/opt/gc/docker-compose.yml
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DUMP_FILE="${BACKUP_DIR}/gc-${TIMESTAMP}.dump"

# Source env for DB credentials
set -a
# shellcheck source=/dev/null
source /opt/gc/.env
set +a

# pg_dump via the running container
docker compose -f "${COMPOSE_FILE}" exec -T db \
  pg_dump -Fc -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "${DUMP_FILE}"

# Upload to S3 (bucket name from env or default)
BUCKET="${BACKUP_BUCKET:-groundcontrol-backups-catalyst-dev}"
aws s3 cp "${DUMP_FILE}" "s3://${BUCKET}/pg-dumps/gc-${TIMESTAMP}.dump"

# Keep only last 3 local dumps
ls -t "${BACKUP_DIR}"/gc-*.dump 2>/dev/null | tail -n +4 | xargs -r rm

echo "$(date): Backup complete — ${DUMP_FILE}"
