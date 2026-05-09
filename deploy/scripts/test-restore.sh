#!/bin/bash
# Ground Control restore test (GC-P021: verify backups on a recurring basis).
#
# Validates a pg_dump produced by deploy/scripts/backup.sh by restoring it
# into a throwaway apache/age:release_PG16_1.6.0 container and running the
# operational-readiness checks below. Any failure exits non-zero so the
# cron-wrapped run produces a loud log line and a systemd/cron failure
# email.
#
# Checks (all must pass):
#   1. public schema contains at least one table
#   2. flyway_schema_history has recorded migrations
#   3. flyway_schema_history contains V010 (create_age_graph)
#   4. AGE extension is loaded in the restored database
#   5. Core Ground Control tables are present (catches truncated dumps)
#   6. create_graph() succeeds against the restored catalog
#      (proves AGE is operationally usable, not just installed)
#
# Overridable via environment for local dev:
#   BACKUP_DIR         default /data/backups
#   TEST_CONTAINER     default gc-restore-test
#   TEST_PORT          default 15432
#   DB_IMAGE           default apache/age:release_PG16_1.6.0
#   SKIP_ENV_FILE=1    skip sourcing /opt/gc/.env (local dev)
#   POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB — only required when
#                      SKIP_ENV_FILE=1
set -euo pipefail

if [ "${SKIP_ENV_FILE:-0}" != "1" ]; then
  set -a
  # shellcheck source=/dev/null
  source /opt/gc/.env
  set +a
fi

BACKUP_DIR="${BACKUP_DIR:-/data/backups}"
TEST_CONTAINER="${TEST_CONTAINER:-gc-restore-test}"
TEST_PORT="${TEST_PORT:-15432}"
DB_IMAGE="${DB_IMAGE:-apache/age:release_PG16_1.6.0}"

: "${POSTGRES_USER:?POSTGRES_USER must be set (via /opt/gc/.env or env override)}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set}"
: "${POSTGRES_DB:?POSTGRES_DB must be set}"

# Use argument or latest local dump.
DUMP_FILE="${1:-$(ls -t "${BACKUP_DIR}"/gc-*.dump 2>/dev/null | head -1)}"
[ -n "${DUMP_FILE}" ] || { echo "ERROR: No backup file found"; exit 1; }
[ -f "${DUMP_FILE}" ] || { echo "ERROR: File not found: ${DUMP_FILE}"; exit 1; }

echo "Testing restore of: ${DUMP_FILE}"

cleanup() {
  echo "Cleaning up test container..."
  docker stop "${TEST_CONTAINER}" 2>/dev/null || true
  docker rm -f "${TEST_CONTAINER}" 2>/dev/null || true
}
trap cleanup EXIT

docker rm -f "${TEST_CONTAINER}" 2>/dev/null || true

echo "Starting temporary PostgreSQL container on port ${TEST_PORT}..."
docker run -d --name "${TEST_CONTAINER}" \
  -e POSTGRES_DB="${POSTGRES_DB}" \
  -e POSTGRES_USER="${POSTGRES_USER}" \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  -p "${TEST_PORT}:5432" \
  "${DB_IMAGE}" >/dev/null

echo "Waiting for test database..."
# Two-phase wait: pg_isready handles initial boot, then require three
# consecutive successful `SELECT 1` queries to confirm we are past the
# apache/age image's post-init restart window.
ready=0
for i in $(seq 1 120); do
  if docker exec "${TEST_CONTAINER}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1 \
     && docker exec "${TEST_CONTAINER}" \
          psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -q -t -A \
          -c "SELECT 1" > /dev/null 2>&1; then
    ready=$((ready + 1))
    [ "${ready}" -ge 3 ] && break
  else
    ready=0
  fi
  [ "$i" -eq 120 ] && { echo "FAIL: Test database did not become ready"; exit 1; }
  sleep 1
done

echo "Restoring into test container..."
docker exec -i "${TEST_CONTAINER}" \
  pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists < "${DUMP_FILE}"

# psql helper — runs as the restored-DB superuser, returns exit 1 on query error
# so failures abort the script via set -e.
psql_scalar() {
  docker exec "${TEST_CONTAINER}" \
    psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -A -v ON_ERROR_STOP=1 \
    -c "$1"
}

echo "Running verification queries..."

TABLE_COUNT="$(psql_scalar "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")"
if [ "${TABLE_COUNT}" -gt 0 ]; then
  echo "PASS: ${TABLE_COUNT} public tables in restored database"
else
  echo "FAIL: No tables found after restore"
  exit 1
fi

MIGRATION_COUNT="$(psql_scalar "SELECT count(*) FROM flyway_schema_history;" 2>/dev/null || echo "0")"
if [ "${MIGRATION_COUNT}" -gt 0 ]; then
  echo "PASS: ${MIGRATION_COUNT} Flyway migrations recorded"
else
  echo "FAIL: flyway_schema_history empty or missing"
  exit 1
fi

# 3. Flyway V010 present (marks the AGE graph bootstrap migration).
V010_PRESENT="$(psql_scalar "SELECT count(*) FROM flyway_schema_history WHERE version = '010';")"
if [ "${V010_PRESENT}" -ge 1 ]; then
  echo "PASS: V010 PRESENT in flyway_schema_history"
else
  echo "FAIL: V010 (create_age_graph) missing from flyway_schema_history"
  exit 1
fi

# 4. AGE extension loaded on the restored cluster.
AGE_EXT="$(psql_scalar "SELECT extname FROM pg_extension WHERE extname = 'age';")"
if [ "${AGE_EXT}" = "age" ]; then
  echo "PASS: AGE extension present"
else
  echo "FAIL: AGE extension not installed in restored database"
  exit 1
fi

# 5. Core Ground Control tables present.
CORE_TABLES="project requirement requirement_relation traceability_link document section threat_model"
missing=""
for t in ${CORE_TABLES}; do
  exists="$(psql_scalar "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='${t}';")"
  if [ "${exists}" -lt 1 ]; then
    missing="${missing} ${t}"
  fi
done
if [ -z "${missing}" ]; then
  echo "PASS: CORE TABLES PRESENT (${CORE_TABLES})"
else
  echo "FAIL: core Ground Control tables missing:${missing}"
  exit 1
fi

# 6. Graph materializable — proves AGE is loadable and create_graph() works
#    against the restored catalog. Uses a throwaway graph name so we never
#    touch a real `requirements` graph that may exist in the dump.
docker exec "${TEST_CONTAINER}" \
  psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -q -c \
  "LOAD 'age'; SET search_path = ag_catalog, \"\$user\", public; SELECT create_graph('requirements_verify'); SELECT drop_graph('requirements_verify', true);"
echo "PASS: GRAPH MATERIALIZABLE via create_graph('requirements_verify')"

echo ""
echo "$(date): Restore test PASSED — ${DUMP_FILE}"
