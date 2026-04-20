#!/bin/bash
# Assert that the Ground Control backup/restore infrastructure meets GC-P021:
#   - backup cadence >= 3/day
#   - local_retention_count guarantees >= 24h of backups (>= 4 files at 3x/day)
#   - restore-test cron runs at least daily
#   - test-restore.sh contains the AGE/core-table/graph/V010 sentinel checks
#   - deploy/scripts/*.sh and deploy/terraform/modules/compute/user-data.sh.tftpl
#     remain in sync on cadence + verification details
#
# This script is intentionally structural (grep-based) rather than a full
# Terraform parse — the goal is a fast, dependency-free gate that runs in
# pre-commit, `make policy`, and CI. It fails loudly the moment the repo
# drifts away from GC-P021 defaults.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BACKUP_MODULE_VARS="${REPO_ROOT}/deploy/terraform/modules/backup/variables.tf"
DEV_ENV_VARS="${REPO_ROOT}/deploy/terraform/environments/dev/variables.tf"
USER_DATA_TFTPL="${REPO_ROOT}/deploy/terraform/modules/compute/user-data.sh.tftpl"
TFVARS_EXAMPLE="${REPO_ROOT}/deploy/terraform/environments/dev/terraform.tfvars.example"
TEST_RESTORE_SCRIPT="${REPO_ROOT}/deploy/scripts/test-restore.sh"
BACKUP_SCRIPT="${REPO_ROOT}/deploy/scripts/backup.sh"
INSTALL_OPS_SCRIPT="${REPO_ROOT}/deploy/scripts/install-ops-scripts.sh"
CI_WORKFLOW="${REPO_ROOT}/.github/workflows/ci.yml"

errors=0
fail() {
  echo "FAIL: $*" >&2
  errors=$((errors + 1))
}

# 1. Backup module + dev env root defaults. The live deployment only sees
#    the dev env's forwarded default, so both layers must carry GC-P021 values.
for file in "${BACKUP_MODULE_VARS}" "${DEV_ENV_VARS}"; do
  label="${file#${REPO_ROOT}/}"
  if ! grep -q 'default *= *"0 3,11,19 \* \* \*"' "${file}"; then
    fail "backup_cron default in ${label} must be \"0 3,11,19 * * *\" (3x/day per GC-P021)"
  fi
  retention_default="$(awk '
    /^variable "local_retention_count"/ { inblock = 1 }
    inblock && /default *= *[0-9]+/ {
      match($0, /default *= *[0-9]+/); value = substr($0, RSTART, RLENGTH);
      gsub(/[^0-9]/, "", value); print value; exit
    }
  ' "${file}")"
  if [[ -z "${retention_default}" ]]; then
    fail "could not parse local_retention_count default from ${label}"
  elif [[ "${retention_default}" -lt 4 ]]; then
    fail "local_retention_count default in ${label} is ${retention_default}; GC-P021 requires >= 4 (>= 24h at 3x/day cadence)"
  fi
done

# 2. user-data cron lines: backup schedule + daily restore test.
if ! grep -q '/etc/cron.d/gc-backup' "${USER_DATA_TFTPL}"; then
  fail "${USER_DATA_TFTPL#${REPO_ROOT}/} no longer writes /etc/cron.d/gc-backup"
fi
if ! grep -q '0 5 \* \* \* root /opt/gc/test-restore.sh' "${USER_DATA_TFTPL}"; then
  fail "restore-test cron in ${USER_DATA_TFTPL#${REPO_ROOT}/} must run daily (0 5 * * *) per GC-P021"
fi
if grep -q '0 5 \* \* 0 root /opt/gc/test-restore.sh' "${USER_DATA_TFTPL}"; then
  fail "weekly restore-test cron (0 5 * * 0) still present in ${USER_DATA_TFTPL#${REPO_ROOT}/}; GC-P021 requires >= daily verification"
fi

# 3. tfvars.example must document the new defaults so operators aren't misled.
if ! grep -q 'backup_cron *= *"0 3,11,19 \* \* \*"' "${TFVARS_EXAMPLE}"; then
  fail "${TFVARS_EXAMPLE#${REPO_ROOT}/} must show backup_cron = \"0 3,11,19 * * *\" as the example value"
fi
if ! grep -q 'local_retention_count *= *4' "${TFVARS_EXAMPLE}"; then
  fail "${TFVARS_EXAMPLE#${REPO_ROOT}/} must show local_retention_count = 4 as the example value"
fi

# 4. test-restore.sh, the inlined user-data copy, AND the canonical installer
#    must all carry the new checks. Sentinel strings are stable across all
#    three files so drift between them is caught here.
for file in "${TEST_RESTORE_SCRIPT}" "${USER_DATA_TFTPL}" "${INSTALL_OPS_SCRIPT}"; do
  label="${file#${REPO_ROOT}/}"
  grep -q 'extname *= *.age.' "${file}"           || fail "${label} missing AGE extension load check (pg_extension query)"
  grep -q 'AGE extension present' "${file}"       || fail "${label} missing AGE-extension sentinel log line"
  grep -q 'CORE TABLES PRESENT' "${file}"         || fail "${label} missing core-tables sentinel log line"
  grep -q 'GRAPH MATERIALIZABLE' "${file}"        || fail "${label} missing graph-materialize sentinel log line"
  grep -q 'V010 PRESENT' "${file}"                || fail "${label} missing V010 Flyway-checksum sentinel log line"
  grep -q "create_graph('requirements_verify')"   "${file}" \
    || fail "${label} must invoke create_graph('requirements_verify') to prove AGE is operational"
done

# 5. Keep the repo-copy scripts in sync with the user-data template on cadence
#    comments so reviewers can see them together in diffs.
if ! grep -q 'GC-P021' "${BACKUP_SCRIPT}"; then
  fail "${BACKUP_SCRIPT#${REPO_ROOT}/} header must reference GC-P021 so the policy anchor is visible in the script itself"
fi
if ! grep -q 'GC-P021' "${TEST_RESTORE_SCRIPT}"; then
  fail "${TEST_RESTORE_SCRIPT#${REPO_ROOT}/} header must reference GC-P021 so the policy anchor is visible in the script itself"
fi
if ! grep -q 'GC-P021' "${INSTALL_OPS_SCRIPT}"; then
  fail "${INSTALL_OPS_SCRIPT#${REPO_ROOT}/} must reference GC-P021 so the policy anchor is visible in the installer"
fi

# 6. The CI deploy job must run install-ops-scripts.sh on every deploy so
#    the live EC2 instance picks up /opt/gc script updates. This is the
#    fix for codex P1: ignore_changes=[user_data] blocks Terraform from
#    propagating script edits.
if [ -f "${CI_WORKFLOW}" ]; then
  if ! grep -q 'deploy/scripts/install-ops-scripts.sh' "${CI_WORKFLOW}"; then
    fail "${CI_WORKFLOW#${REPO_ROOT}/} must run deploy/scripts/install-ops-scripts.sh during the deploy job (GC-P021 script rollout)"
  fi
  if ! grep -q 'ssm-install-ops' "${CI_WORKFLOW}"; then
    fail "${CI_WORKFLOW#${REPO_ROOT}/} must wire the install-ops step (id=ssm-install-ops) in the deploy job"
  fi
fi

# 7. Run the installer against an ephemeral prefix and prove it (a) refuses
#    non-compliant inputs and (b) writes the expected artifacts. This catches
#    bit-rot the grep-based sentinel checks cannot see.
tmp_prefix="$(mktemp -d -t gc-install-ops-XXXXXXXX)"
trap 'rm -rf "${tmp_prefix}"' EXIT

if GC_INSTALL_PREFIX="${tmp_prefix}" \
   GC_BACKUP_BUCKET=test-bucket \
   GC_BACKUP_CRON='0 3 * * *' \
   GC_LOCAL_RETENTION_COUNT=4 \
   bash "${INSTALL_OPS_SCRIPT}" >/dev/null 2>&1; then
  fail "install-ops-scripts.sh accepted 1x/day cron (GC-P021 requires >= 3x/day)"
fi
if GC_INSTALL_PREFIX="${tmp_prefix}" \
   GC_BACKUP_BUCKET=test-bucket \
   GC_BACKUP_CRON='0 3,11,19 * * *' \
   GC_LOCAL_RETENTION_COUNT=3 \
   bash "${INSTALL_OPS_SCRIPT}" >/dev/null 2>&1; then
  fail "install-ops-scripts.sh accepted retention=3 (GC-P021 requires >= 4)"
fi
rm -rf "${tmp_prefix}"
mkdir -p "${tmp_prefix}"

if ! GC_INSTALL_PREFIX="${tmp_prefix}" \
     GC_BACKUP_BUCKET=test-bucket \
     GC_BACKUP_CRON='0 3,11,19 * * *' \
     GC_LOCAL_RETENTION_COUNT=4 \
     bash "${INSTALL_OPS_SCRIPT}" >/dev/null 2>&1; then
  fail "install-ops-scripts.sh rejected compliant inputs — smoke test failed"
else
  for f in backup.sh restore.sh test-restore.sh watchdog.sh; do
    [ -x "${tmp_prefix}/opt/gc/${f}" ] \
      || fail "install-ops-scripts.sh did not install executable ${f}"
  done
  grep -q 's3://test-bucket/pg-dumps/' "${tmp_prefix}/opt/gc/backup.sh" \
    || fail "installed backup.sh missing substituted GC_BACKUP_BUCKET"
  grep -q 'tail -n +$((4 + 1))' "${tmp_prefix}/opt/gc/backup.sh" \
    || fail "installed backup.sh missing substituted GC_LOCAL_RETENTION_COUNT"
  grep -q '^0 3,11,19 \* \* \* root /opt/gc/backup.sh' "${tmp_prefix}/etc/cron.d/gc-backup" \
    || fail "installed gc-backup cron entry does not reflect GC_BACKUP_CRON"
  grep -q '^0 5 \* \* \* root /opt/gc/test-restore.sh' "${tmp_prefix}/etc/cron.d/gc-restore-test" \
    || fail "installed gc-restore-test cron must run daily at 05:00 UTC"
fi

if [[ "${errors}" -gt 0 ]]; then
  echo "assert-backup-policy.sh: ${errors} failure(s)" >&2
  exit 1
fi

echo "assert-backup-policy.sh: OK"
