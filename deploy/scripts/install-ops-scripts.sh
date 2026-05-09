#!/bin/bash
# Install /opt/gc/{backup,restore,test-restore,watchdog}.sh and the three
# /etc/cron.d entries that drive GC-P021. Canonical installer.
#
# Runs on the EC2 instance in two paths:
#   1. First boot — user-data writes this file and calls it.
#   2. Every main-branch deploy — the CI `deploy` job base64-transports
#      this file via SSM SendCommand so the live instance picks up new
#      scripts even though `ignore_changes = [user_data]` on the EC2
#      resource prevents Terraform from rewriting user-data.
#
# Idempotent; safe to re-run. Exits non-zero if the requested parameters
# violate GC-P021 floors.
#
# Required env:
#   GC_BACKUP_BUCKET              S3 bucket that holds pg-dumps/
#   GC_BACKUP_CRON                cron expression; must be >= 3/day
#   GC_LOCAL_RETENTION_COUNT      integer >= 4 (>= 24h at 3x/day)
#
# Canonical-sync guardrail:
#   scripts/assert-backup-policy.sh greps this file and the
#   deploy/scripts/*.sh copies for the same GC-P021 sentinels
#   (AGE_EXT, CORE TABLES PRESENT, V010 PRESENT, GRAPH MATERIALIZABLE,
#   create_graph('requirements_verify')). Any drift fails pre-commit.
set -euo pipefail

: "${GC_BACKUP_BUCKET:?GC_BACKUP_BUCKET is required}"
: "${GC_BACKUP_CRON:?GC_BACKUP_CRON is required}"
: "${GC_LOCAL_RETENTION_COUNT:?GC_LOCAL_RETENTION_COUNT is required}"

case "${GC_LOCAL_RETENTION_COUNT}" in
  ''|*[!0-9]*)
    echo "ERROR: GC_LOCAL_RETENTION_COUNT must be a non-negative integer (got '${GC_LOCAL_RETENTION_COUNT}')" >&2
    exit 1
    ;;
esac
if [ "${GC_LOCAL_RETENTION_COUNT}" -lt 4 ]; then
  echo "ERROR: GC_LOCAL_RETENTION_COUNT=${GC_LOCAL_RETENTION_COUNT} is below the GC-P021 floor (4)" >&2
  exit 1
fi

# GC-P021 requires >= 3 backups/day. Validate the cron minute/hour fields
# declare at least three distinct run times per day.
cron_minute="$(echo "${GC_BACKUP_CRON}" | awk '{print $1}')"
cron_hour="$(echo "${GC_BACKUP_CRON}" | awk '{print $2}')"
runs_per_day="$(python3 - "${cron_minute}" "${cron_hour}" <<'PY'
import sys

minute, hour = sys.argv[1], sys.argv[2]

def enumerate_field(expr, low, high):
    if expr == "*":
        return set(range(low, high + 1))
    out = set()
    for part in expr.split(","):
        step = 1
        if "/" in part:
            part, step_str = part.split("/", 1)
            step = int(step_str)
        if part == "*":
            lo, hi = low, high
        elif "-" in part:
            lo_str, hi_str = part.split("-", 1)
            lo, hi = int(lo_str), int(hi_str)
        else:
            lo = hi = int(part)
        out.update(range(lo, hi + 1, step))
    return out

minutes = enumerate_field(minute, 0, 59)
hours = enumerate_field(hour, 0, 23)
print(len(minutes) * len(hours))
PY
)"
if [ "${runs_per_day}" -lt 3 ]; then
  echo "ERROR: GC_BACKUP_CRON='${GC_BACKUP_CRON}' fires ${runs_per_day} time(s)/day; GC-P021 floor is 3" >&2
  exit 1
fi

GC_INSTALL_PREFIX="${GC_INSTALL_PREFIX:-}"
GC_OPT_DIR="${GC_INSTALL_PREFIX}/opt/gc"
GC_CRON_DIR="${GC_INSTALL_PREFIX}/etc/cron.d"
install -d "${GC_OPT_DIR}"
install -d "${GC_CRON_DIR}"

# --- ${GC_OPT_DIR}/backup.sh --------------------------------------------------
cat > ${GC_OPT_DIR}/backup.sh <<'BACKUPEOF'
#!/bin/bash
# Ground Control database backup — pg_dump to local + S3 (GC-P021).
# Rewritten by deploy/scripts/install-ops-scripts.sh; do not edit by hand.
set -euo pipefail

set -a
# shellcheck source=/dev/null
source /opt/gc/.env
set +a

BACKUP_DIR=/data/backups
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DUMP_FILE="${BACKUP_DIR}/gc-${TIMESTAMP}.dump"

docker compose -f /opt/gc/docker-compose.yml exec -T db \
  pg_dump -Fc -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "${DUMP_FILE}"

aws s3 cp "${DUMP_FILE}" "s3://__GC_BACKUP_BUCKET__/pg-dumps/gc-${TIMESTAMP}.dump"

# Retention: keep at least GC-P021's 24h floor.
ls -t "${BACKUP_DIR}"/gc-*.dump 2>/dev/null | tail -n +$((__GC_LOCAL_RETENTION_COUNT__ + 1)) | xargs -r rm

echo "$(date): Backup complete — ${DUMP_FILE}"
BACKUPEOF
sed -i \
  -e "s|__GC_BACKUP_BUCKET__|${GC_BACKUP_BUCKET}|g" \
  -e "s|__GC_LOCAL_RETENTION_COUNT__|${GC_LOCAL_RETENTION_COUNT}|g" \
  ${GC_OPT_DIR}/backup.sh
chmod +x ${GC_OPT_DIR}/backup.sh

# --- ${GC_OPT_DIR}/restore.sh -------------------------------------------------
cat > ${GC_OPT_DIR}/restore.sh <<'RESTOREEOF'
#!/bin/bash
# Ground Control database restore — from local dump or S3 (GC-P021 recovery).
# Rewritten by deploy/scripts/install-ops-scripts.sh; do not edit by hand.
set -euo pipefail

set -a
# shellcheck source=/dev/null
source /opt/gc/.env
set +a

BACKUP_DIR=/data/backups
COMPOSE_FILE=/opt/gc/docker-compose.yml
BUCKET="${BACKUP_BUCKET:-__GC_BACKUP_BUCKET__}"
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

[ $# -eq 0 ] && { usage; exit 1; }

MODE=""
FILE_ARG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --list) MODE="list"; shift ;;
    --from-file)
      [ $# -ge 2 ] || { echo "ERROR: --from-file requires a path"; usage; exit 1; }
      MODE="file"; FILE_ARG="$2"; shift 2 ;;
    --from-s3)
      [ $# -ge 2 ] || { echo "ERROR: --from-s3 requires an S3 key"; usage; exit 1; }
      MODE="s3"; FILE_ARG="$2"; shift 2 ;;
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
RESTOREEOF
sed -i "s|__GC_BACKUP_BUCKET__|${GC_BACKUP_BUCKET}|g" ${GC_OPT_DIR}/restore.sh
chmod +x ${GC_OPT_DIR}/restore.sh

# --- ${GC_OPT_DIR}/test-restore.sh --------------------------------------------
cat > ${GC_OPT_DIR}/test-restore.sh <<'TESTRESTOREEOF'
#!/bin/bash
# Ground Control restore test (GC-P021 recurring restore verification).
# Rewritten by deploy/scripts/install-ops-scripts.sh; do not edit by hand.
#
# Validates a pg_dump produced by /opt/gc/backup.sh by restoring it into
# a throwaway apache/age:release_PG16_1.6.0 container and running the
# operational-readiness checks below. Any failure exits non-zero so the
# cron-wrapped run produces a loud log line.
#
# Checks (all must pass):
#   1. public schema contains at least one table
#   2. flyway_schema_history has recorded migrations
#   3. flyway_schema_history contains V010 (create_age_graph)
#   4. AGE extension is loaded in the restored database
#   5. Core Ground Control tables are present (catches truncated dumps)
#   6. create_graph() succeeds against the restored catalog
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

V010_PRESENT="$(psql_scalar "SELECT count(*) FROM flyway_schema_history WHERE version = '010';")"
if [ "${V010_PRESENT}" -ge 1 ]; then
  echo "PASS: V010 PRESENT in flyway_schema_history"
else
  echo "FAIL: V010 (create_age_graph) missing from flyway_schema_history"
  exit 1
fi

AGE_EXT="$(psql_scalar "SELECT extname FROM pg_extension WHERE extname = 'age';")"
if [ "${AGE_EXT}" = "age" ]; then
  echo "PASS: AGE extension present"
else
  echo "FAIL: AGE extension not installed in restored database"
  exit 1
fi

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

docker exec "${TEST_CONTAINER}" \
  psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -q -c \
  "LOAD 'age'; SET search_path = ag_catalog, \"\$user\", public; SELECT create_graph('requirements_verify'); SELECT drop_graph('requirements_verify', true);"
echo "PASS: GRAPH MATERIALIZABLE via create_graph('requirements_verify')"

echo ""
echo "$(date): Restore test PASSED — ${DUMP_FILE}"
TESTRESTOREEOF
chmod +x ${GC_OPT_DIR}/test-restore.sh

# --- ${GC_OPT_DIR}/watchdog.sh ------------------------------------------------
cat > ${GC_OPT_DIR}/watchdog.sh <<'WATCHEOF'
#!/bin/bash
# Ground Control backend watchdog — cron restarts backend on health failure.
# Rewritten by deploy/scripts/install-ops-scripts.sh; do not edit by hand.
set -uo pipefail

HEALTH=$(curl -sf http://localhost:8000/actuator/health 2>/dev/null || echo "DOWN")

if echo "${HEALTH}" | grep -q '"status":"UP"'; then
  exit 0
fi

echo "$(date): Health check failed (${HEALTH}), restarting backend..." >> /var/log/gc-watchdog.log
cd /opt/gc && docker compose restart backend
WATCHEOF
chmod +x ${GC_OPT_DIR}/watchdog.sh

# --- Cron files ---------------------------------------------------------
# Cron entries reference the production install path (/opt/gc) regardless of
# GC_INSTALL_PREFIX. The prefix only affects where the cron *files* land so
# tests can assert on content without needing /etc/cron.d write access.
cat > "${GC_CRON_DIR}/gc-backup" <<EOF
${GC_BACKUP_CRON} root /opt/gc/backup.sh >> /var/log/gc-backup.log 2>&1
EOF
cat > "${GC_CRON_DIR}/gc-restore-test" <<'EOF'
0 5 * * * root /opt/gc/test-restore.sh >> /var/log/gc-restore-test.log 2>&1
EOF
cat > "${GC_CRON_DIR}/gc-watchdog" <<'EOF'
*/5 * * * * root /opt/gc/watchdog.sh
EOF
chmod 644 "${GC_CRON_DIR}/gc-backup" "${GC_CRON_DIR}/gc-restore-test" "${GC_CRON_DIR}/gc-watchdog"

echo "$(date): install-ops-scripts.sh installed /opt/gc + /etc/cron.d (bucket=${GC_BACKUP_BUCKET}, cron='${GC_BACKUP_CRON}', retention=${GC_LOCAL_RETENTION_COUNT})"
