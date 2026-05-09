#!/bin/bash
# Ground Control database backup — pg_dump to local + S3 (GC-P021).
# Runs via cron. Cadence (>= 3/day) and local retention (>= 24h) are
# enforced by deploy/terraform/modules/backup/variables.tf defaults;
# scripts/assert-backup-policy.sh asserts those defaults stay in place.
set -euo pipefail

# Source env for DB credentials
set -a
# shellcheck source=/dev/null
source /opt/gc/.env
set +a

BACKUP_DIR=/data/backups
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DUMP_FILE="${BACKUP_DIR}/gc-${TIMESTAMP}.dump"
BUCKET="${BACKUP_BUCKET:-groundcontrol-backups-catalyst-dev}"
KEEP="${LOCAL_RETENTION_COUNT:-3}"

# pg_dump via the running container
docker compose -f /opt/gc/docker-compose.yml exec -T db \
  pg_dump -Fc -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "${DUMP_FILE}"

# Upload to S3
aws s3 cp "${DUMP_FILE}" "s3://${BUCKET}/pg-dumps/gc-${TIMESTAMP}.dump"

# Rotate: keep only last N local dumps
ls -t "${BACKUP_DIR}"/gc-*.dump 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm

echo "$(date): Backup complete — ${DUMP_FILE}"
