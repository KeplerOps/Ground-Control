#!/bin/bash
# Assert that the Ground Control backup/restore infrastructure meets GC-P021.
#
# History: this script grew up around the AWS EC2 deployment (ADR-018) and
# encoded structural assertions across deploy/terraform/modules/, the
# user-data.sh.tftpl cloud-init template, the tfvars.example, the canonical
# /opt/gc backup scripts, and the CI deploy job's SSM SendCommand path.
# When ADR-030 moved production to red-dragon (Hetzner, on-prem) the AWS
# resources were destroyed, deploy/terraform/ was removed from the repo,
# and the CI deploy job switched from SSM SendCommand to SSH.
#
# Until the red-dragon backup mechanism (rsync-over-tailnet to aurora) is
# wired up in a follow-up, the only GC-P021 anchors that still apply are
# the script-level ones in deploy/scripts/. The AWS-specific assertions
# below have been removed; the smoke-test of install-ops-scripts.sh is
# preserved because the installer itself still validates >= 3x/day cadence
# and >= 4-file retention against its inputs (the policy floor on the
# installer remains the right gate for any future caller).
#
# When the red-dragon mechanism lands, this script regrows assertions for
# the new files (backup cron on the host, rsync target reachability,
# restore-test cadence). The script-level sentinel checks here transfer
# directly.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

TEST_RESTORE_SCRIPT="${REPO_ROOT}/deploy/scripts/test-restore.sh"
BACKUP_SCRIPT="${REPO_ROOT}/deploy/scripts/backup.sh"
INSTALL_OPS_SCRIPT="${REPO_ROOT}/deploy/scripts/install-ops-scripts.sh"

errors=0
fail() {
  echo "FAIL: $*" >&2
  errors=$((errors + 1))
}

# Sentinel checks on the canonical /opt/gc scripts. test-restore.sh and the
# installer must carry the AGE/core-table/graph/V010 verification surface
# so a missing extension or schema regression fails the daily restore test.
for file in "${TEST_RESTORE_SCRIPT}" "${INSTALL_OPS_SCRIPT}"; do
  label="${file#${REPO_ROOT}/}"
  grep -q 'extname *= *.age.' "${file}"           || fail "${label} missing AGE extension load check (pg_extension query)"
  grep -q 'AGE extension present' "${file}"       || fail "${label} missing AGE-extension sentinel log line"
  grep -q 'CORE TABLES PRESENT' "${file}"         || fail "${label} missing core-tables sentinel log line"
  grep -q 'GRAPH MATERIALIZABLE' "${file}"        || fail "${label} missing graph-materialize sentinel log line"
  grep -q 'V010 PRESENT' "${file}"                || fail "${label} missing V010 Flyway-checksum sentinel log line"
  grep -q "create_graph('requirements_verify')"   "${file}" \
    || fail "${label} must invoke create_graph('requirements_verify') to prove AGE is operational"
done

# Repo-copy scripts must reference GC-P021 in a comment header so reviewers
# scanning a diff can see the policy anchor in the script itself.
if ! grep -q 'GC-P021' "${BACKUP_SCRIPT}"; then
  fail "${BACKUP_SCRIPT#${REPO_ROOT}/} header must reference GC-P021 so the policy anchor is visible in the script itself"
fi
if ! grep -q 'GC-P021' "${TEST_RESTORE_SCRIPT}"; then
  fail "${TEST_RESTORE_SCRIPT#${REPO_ROOT}/} header must reference GC-P021 so the policy anchor is visible in the script itself"
fi
if ! grep -q 'GC-P021' "${INSTALL_OPS_SCRIPT}"; then
  fail "${INSTALL_OPS_SCRIPT#${REPO_ROOT}/} must reference GC-P021 so the policy anchor is visible in the installer"
fi

# Run the installer against an ephemeral prefix and prove it (a) refuses
# non-compliant inputs and (b) writes the expected artifacts. The installer
# itself enforces the GC-P021 floor on cadence and retention, so a bug
# there would silently weaken policy regardless of which deployment it
# eventually targets.
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

echo "assert-backup-policy.sh: OK (script-level anchors only — red-dragon mechanism assertions land in the follow-up)"
