#!/bin/bash
# Ground Control restore test — validates backup integrity without production impact
# Spins up a temporary PostgreSQL container, restores, verifies, and tears down.
set -euo pipefail

set -a; source /opt/gc/.env; set +a

BACKUP_DIR=/data/backups
TEST_CONTAINER="gc-restore-test"
TEST_PORT=15432
DB_IMAGE="apache/age:release_PG16_1.6.0"

# Use argument or latest local dump
DUMP_FILE="${1:-$(ls -t "${BACKUP_DIR}"/gc-*.dump 2>/dev/null | head -1)}"
[ -n "${DUMP_FILE}" ] || { echo "ERROR: No backup file found"; exit 1; }
[ -f "${DUMP_FILE}" ] || { echo "ERROR: File not found: ${DUMP_FILE}"; exit 1; }

echo "Testing restore of: ${DUMP_FILE}"

# Always clean up on exit
cleanup() {
  echo "Cleaning up test container..."
  docker stop "${TEST_CONTAINER}" 2>/dev/null || true
  docker rm -f "${TEST_CONTAINER}" 2>/dev/null || true
}
trap cleanup EXIT

# Remove any leftover test container
docker rm -f "${TEST_CONTAINER}" 2>/dev/null || true

echo "Starting temporary PostgreSQL container on port ${TEST_PORT}..."
docker run -d --name "${TEST_CONTAINER}" \
  -e POSTGRES_DB="${POSTGRES_DB}" \
  -e POSTGRES_USER="${POSTGRES_USER}" \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  -p "${TEST_PORT}:5432" \
  "${DB_IMAGE}"

echo "Waiting for test database..."
for i in $(seq 1 30); do
  if docker exec "${TEST_CONTAINER}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1; then
    break
  fi
  [ "$i" -eq 30 ] && { echo "FAIL: Test database did not become ready"; exit 1; }
  sleep 1
done

echo "Restoring into test container..."
docker exec -i "${TEST_CONTAINER}" \
  pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists < "${DUMP_FILE}"

echo "Running verification queries..."

TABLE_COUNT=$(docker exec "${TEST_CONTAINER}" \
  psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -A \
  -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")

if [ "${TABLE_COUNT}" -gt 0 ]; then
  echo "PASS: ${TABLE_COUNT} public tables in restored database"
else
  echo "FAIL: No tables found after restore"
  exit 1
fi

MIGRATION_COUNT=$(docker exec "${TEST_CONTAINER}" \
  psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -A \
  -c "SELECT count(*) FROM flyway_schema_history;" 2>/dev/null || echo "0")

if [ "${MIGRATION_COUNT}" -gt 0 ]; then
  echo "PASS: ${MIGRATION_COUNT} Flyway migrations recorded"
else
  echo "WARN: No Flyway migration history"
fi

echo ""
echo "$(date): Restore test PASSED — ${DUMP_FILE}"
