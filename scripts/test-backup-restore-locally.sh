#!/bin/bash
# Local end-to-end exerciser for the GC-P021 backup/restore loop.
#
# Workflow:
#   1. Start a fresh apache/age PostgreSQL container on a non-standard port.
#   2. Apply the repo's Flyway migrations (V001..V058) in version order.
#   3. pg_dump that container to produce a realistic dump.
#   4. Run deploy/scripts/test-restore.sh against the dump via env overrides.
#   5. Assert every GC-P021 sentinel check appears in the output.
#
# Self-contained — does not require the local docker-compose stack or a
# built backend image. Always tears down the two ephemeral containers.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

SEED_CONTAINER="gc-backup-seed-local"
TEST_CONTAINER="gc-restore-test-local"
SEED_PORT="${SEED_PORT:-25432}"
TEST_PORT="${TEST_PORT:-25433}"
DB_IMAGE="apache/age:release_PG16_1.6.0"
POSTGRES_USER="gc"
POSTGRES_PASSWORD="gc"
POSTGRES_DB="ground_control"
MIGRATIONS_DIR="${REPO_ROOT}/backend/src/main/resources/db/migration"

WORKDIR="$(mktemp -d -t gc-backup-local-XXXXXXXX)"
DUMP_FILE="${WORKDIR}/gc-local.dump"
LOG_FILE="${WORKDIR}/test-restore.log"

cleanup() {
  docker rm -f "${SEED_CONTAINER}" >/dev/null 2>&1 || true
  docker rm -f "${TEST_CONTAINER}" >/dev/null 2>&1 || true
  rm -rf "${WORKDIR}"
}
trap cleanup EXIT

docker rm -f "${SEED_CONTAINER}" >/dev/null 2>&1 || true

echo "Starting seed ${DB_IMAGE} on port ${SEED_PORT}..."
docker run -d --name "${SEED_CONTAINER}" \
  -e POSTGRES_DB="${POSTGRES_DB}" \
  -e POSTGRES_USER="${POSTGRES_USER}" \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  -p "${SEED_PORT}:5432" \
  "${DB_IMAGE}" >/dev/null

# Two-phase wait: the apache/age image briefly restarts after first boot.
# Require three consecutive successful SELECT 1 calls so migrations don't
# race against the restart.
ready=0
for _ in $(seq 1 120); do
  if docker exec "${SEED_CONTAINER}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1 \
     && docker exec "${SEED_CONTAINER}" \
          psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -q -t -A \
          -c "SELECT 1" >/dev/null 2>&1; then
    ready=$((ready + 1))
    [ "${ready}" -ge 3 ] && break
  else
    ready=0
  fi
  sleep 1
done
if [ "${ready}" -lt 3 ]; then
  echo "ERROR: seed database did not stabilize within 120s" >&2
  exit 1
fi

echo "Creating flyway_schema_history and applying migrations..."
docker exec -i "${SEED_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE IF NOT EXISTS flyway_schema_history (
  installed_rank integer NOT NULL PRIMARY KEY,
  version varchar(50),
  description varchar(200) NOT NULL,
  type varchar(20) NOT NULL,
  script varchar(1000) NOT NULL,
  checksum integer,
  installed_by varchar(100) NOT NULL,
  installed_on timestamp NOT NULL DEFAULT now(),
  execution_time integer NOT NULL,
  success boolean NOT NULL
);
SQL

rank=0
while IFS= read -r -d '' file; do
  rank=$((rank + 1))
  base="$(basename "${file}")"
  version="${base%%__*}"      # e.g. V010
  version="${version#V}"       # e.g. 010
  desc="${base#V*__}"
  desc="${desc%.sql}"
  desc="${desc//_/ }"
  echo "  apply V${version} — ${desc}"
  docker exec -i "${SEED_CONTAINER}" \
    psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 < "${file}"
  docker exec "${SEED_CONTAINER}" \
    psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -c \
    "INSERT INTO flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (${rank}, '${version}', '${desc}', 'SQL', '${base}', 0, '${POSTGRES_USER}', 0, true);" \
    >/dev/null
done < <(find "${MIGRATIONS_DIR}" -maxdepth 1 -name 'V*.sql' -print0 | sort -z)

echo "Creating pg_dump at ${DUMP_FILE}..."
docker exec "${SEED_CONTAINER}" \
  pg_dump -Fc -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "${DUMP_FILE}"

echo "Running test-restore.sh against the dump..."
BACKUP_DIR="${WORKDIR}" \
  TEST_CONTAINER="${TEST_CONTAINER}" \
  TEST_PORT="${TEST_PORT}" \
  DB_IMAGE="${DB_IMAGE}" \
  POSTGRES_DB="${POSTGRES_DB}" \
  POSTGRES_USER="${POSTGRES_USER}" \
  POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  SKIP_ENV_FILE=1 \
  bash "${REPO_ROOT}/deploy/scripts/test-restore.sh" "${DUMP_FILE}" \
  | tee "${LOG_FILE}"

errors=0
assert_log() {
  local needle="$1"
  if ! grep -qF "${needle}" "${LOG_FILE}"; then
    echo "FAIL: expected '${needle}' in test-restore output" >&2
    errors=$((errors + 1))
  fi
}
assert_log 'PASS: AGE extension present'
assert_log 'PASS: CORE TABLES PRESENT'
assert_log 'PASS: GRAPH MATERIALIZABLE'
assert_log 'PASS: V010 PRESENT'
assert_log 'Restore test PASSED'

if [[ "${errors}" -gt 0 ]]; then
  echo "test-backup-restore-locally.sh: ${errors} sentinel check(s) missing — GC-P021 verification incomplete" >&2
  exit 1
fi

echo "test-backup-restore-locally.sh: OK"
