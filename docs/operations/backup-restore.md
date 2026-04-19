# Backup and Restore Runbook

This runbook gets Ground Control back to an operational state after data
loss or host failure. It assumes:

- You have AWS CLI or console access to account `516608939870`
  (`us-east-2`). The AWS CLI profile this project uses is `catalyst-dev`.
- You are enrolled in the Tailscale tailnet that owns `gc-dev`.
- You have SSH access to `gc-dev` via Tailscale SSH (no SSH key is
  required — Tailscale authenticates the connection).
- You have **no prior exposure** to Ground Control internals. Everything
  you need is either in this document or linked inline.

If any prerequisite above is missing, stop and request it from the
repository owner before continuing.

Anchors:

- Policy: Ground Control requirement GC-P021 (filed as GitHub
  issue [#533](https://github.com/KeplerOps/Ground-Control/issues/533));
  this runbook satisfies its "documented such that recovery can be
  performed without prior knowledge" clause.
- Architecture context: [ADR-018](../../architecture/adrs/018-aws-ec2-deployment.md),
  [ADR-025](../../architecture/adrs/025-backup-policy.md).

---

## 1. What is backed up

Ground Control runs on one EC2 instance (tag `Name=groundcontrol-ec2`)
in `us-east-2`. Two backup layers protect the data:

| Layer | What | Where | Cadence | Retention |
|-------|------|-------|---------|-----------|
| pg_dump (logical) | Custom-format Postgres dump of the `ground_control` database | EBS `/data/backups/` on the instance **and** `s3://groundcontrol-backups-catalyst-dev/pg-dumps/` | 03:00, 11:00, 19:00 UTC (3×/day) | ≥ 24 h local (4 dump files), 30 days S3 |
| EBS snapshot (block) | Full `/data` volume including Postgres data directory and local dumps | AWS EBS snapshots tagged `Service=ground-control` | Daily 04:00 UTC | 7 snapshots |

The relational tables are the authoritative source of truth. The Apache
AGE graph stored in `ag_catalog` is derivative — it is rebuilt from the
relational tables by
`POST /api/v1/admin/graph/materialize`
(code: `AgeGraphService.materializeGraph()`).

Secrets the running application needs are stored in AWS SSM Parameter
Store under `/gc/dev/*` (not on the instance, not in backups). See
[§ 7 Rotating credentials](#7-rotating-credentials).

---

## 2. Locating resources

Run these from any machine with AWS CLI and the `catalyst-dev` profile.

```bash
# The live EC2 instance.
aws ec2 describe-instances \
  --profile catalyst-dev --region us-east-2 \
  --filters "Name=tag:Name,Values=groundcontrol-ec2" \
  --query 'Reservations[].Instances[].[InstanceId,State.Name,PrivateIpAddress]' \
  --output table

# The most recent S3 pg_dumps.
aws s3 ls s3://groundcontrol-backups-catalyst-dev/pg-dumps/ \
  --profile catalyst-dev --recursive | sort | tail -10

# The most recent EBS snapshots of the data volume.
aws ec2 describe-snapshots \
  --profile catalyst-dev --region us-east-2 \
  --filters "Name=tag:Service,Values=ground-control" \
  --query 'Snapshots[].[SnapshotId,StartTime,VolumeSize,State]' \
  --output table

# SSM parameters the instance uses (names only; values are SecureString).
aws ssm describe-parameters \
  --profile catalyst-dev --region us-east-2 \
  --parameter-filters "Key=Name,Option=BeginsWith,Values=/gc/dev/" \
  --query 'Parameters[].Name' --output table
```

Tailscale hostname: `gc-dev`. SSH once to prove access:

```bash
ssh gc-dev 'echo ok && docker compose -f /opt/gc/docker-compose.yml ps'
```

---

## 3. Recovery scenarios

Pick the scenario that matches what failed.

### 3a. Data corruption or accidental delete (instance still running)

Use this when the database is up but the data is wrong (bad migration,
user error, malicious delete).

```bash
ssh gc-dev /opt/gc/restore.sh --list
```

Choose a dump whose timestamp is **before** the corruption. Then:

```bash
ssh gc-dev /opt/gc/restore.sh --from-s3 pg-dumps/gc-<TIMESTAMP>.dump
```

The script takes a safety backup first and prompts for confirmation (use
`--yes` to skip the prompt from an automation). After it completes,
continue with [§ 5 Rematerialize the AGE graph](#5-rematerialize-the-age-graph)
and [§ 6 Post-restore verification](#6-post-restore-verification).

### 3b. Instance failed, data volume intact

Use this if the EC2 instance is stopped/terminated but the EBS data
volume still exists (the `/data` volume is `skip_destroy = true` in
Terraform and survives instance replacement).

1. Note the existing data volume ID:

    ```bash
    aws ec2 describe-volumes \
      --profile catalyst-dev --region us-east-2 \
      --filters "Name=tag:Name,Values=groundcontrol-data" \
      --query 'Volumes[].[VolumeId,State,AvailabilityZone]' --output table
    ```

2. Launch a replacement instance via Terraform:

    ```bash
    cd deploy/terraform/environments/dev
    terraform apply
    ```

    The `user-data.sh.tftpl` reinstalls Docker, pulls the container
    image, and mounts `/data`. Because the volume still contains
    `/data/postgres/`, Postgres comes back with all prior state.

3. Verify as in [§ 6](#6-post-restore-verification). No AGE
   rematerialize is required because the `ag_catalog` tables are on the
   preserved volume.

### 3c. Full loss — rebuild from backups

Use this if both the instance and the data volume are lost, or if you
are rebuilding into a new account.

1. Choose a source of truth. Pick **one**:
    - The latest valid EBS snapshot (preferred; includes everything on
      `/data`):

       ```bash
       aws ec2 describe-snapshots \
         --profile catalyst-dev --region us-east-2 \
         --filters "Name=tag:Service,Values=ground-control" \
         --query 'Snapshots|sort_by(@,&StartTime)[-3:].[SnapshotId,StartTime]' \
         --output table
       ```

    - The latest valid S3 pg_dump (use this if EBS snapshots are gone):

       ```bash
       aws s3 ls s3://groundcontrol-backups-catalyst-dev/pg-dumps/ \
         --profile catalyst-dev --recursive | sort | tail -3
       ```

2. **EBS-snapshot path**: create a new volume from the snapshot and let
   Terraform attach it:

    ```bash
    aws ec2 create-volume \
      --profile catalyst-dev --region us-east-2 \
      --snapshot-id <snap-id> \
      --availability-zone us-east-2a \
      --volume-type gp3 \
      --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=groundcontrol-data},{Key=Backup,Value=true},{Key=Service,Value=ground-control}]'

    cd deploy/terraform/environments/dev
    terraform import module.compute.aws_ebs_volume.data <new-vol-id>
    terraform apply
    ```

   Terraform drives a fresh EC2 from
   `deploy/terraform/modules/compute/user-data.sh.tftpl` and attaches the
   restored volume. Skip to step 4.

3. **S3 pg_dump path**: let Terraform create a new blank volume, then
   restore the dump:

    ```bash
    cd deploy/terraform/environments/dev
    terraform apply
    # Wait for the instance to finish user-data. Tail /var/log/gc-init.log
    # over Tailscale SSH to watch progress.
    ssh gc-dev 'tail -f /var/log/gc-init.log'

    # Once docker compose is up, pull a dump and restore.
    ssh gc-dev /opt/gc/restore.sh --from-s3 pg-dumps/gc-<TIMESTAMP>.dump --yes
    ```

4. Rematerialize the AGE graph (see [§ 5](#5-rematerialize-the-age-graph)).

5. Verify (see [§ 6](#6-post-restore-verification)).

---

## 4. Manually running backup or restore-test

Useful when preparing for an intentionally-risky change, or when asked
to prove the pipeline works.

```bash
# Take a pg_dump now.
ssh gc-dev /opt/gc/backup.sh

# Run the verification loop against the newest dump.
ssh gc-dev /opt/gc/test-restore.sh

# Tail the recurring-verification log.
ssh gc-dev cat /var/log/gc-restore-test.log
```

Locally (no AWS access required), the equivalent loop is:

```bash
# Requires only Docker.
scripts/test-backup-restore-locally.sh
```

This spins up a fresh `apache/age` Postgres, replays every Flyway
migration, dumps, and runs `deploy/scripts/test-restore.sh` against the
dump.

---

## 5. Rematerialize the AGE graph

After **any** restore from an S3 pg_dump (§ 3a, § 3c path B), trigger the
graph rematerialize. This ensures the AGE graph reflects the restored
relational data even if the dump's `ag_catalog` OIDs drifted from what
the fresh `create_graph('requirements')` allocated during the V010
Flyway migration.

```bash
# From the gc-dev host (reaches localhost:8000):
ssh gc-dev 'curl -sf -X POST http://localhost:8000/api/v1/admin/graph/materialize'

# From a workstation with Tailscale + MagicDNS:
curl -sf -X POST http://gc-dev:8000/api/v1/admin/graph/materialize
```

The endpoint returns HTTP 200 with no body. Confirm the graph is
populated:

```bash
curl -s 'http://gc-dev:8000/api/v1/analysis/dashboard-stats?project=ground-control' \
  | jq '.'
```

If `dashboard-stats` returns non-zero counts for requirements / links,
the restored state is operational.

---

## 6. Post-restore verification

Run **all** of the following from a workstation in the tailnet:

```bash
# 6a. Database is responding.
ssh gc-dev 'docker compose -f /opt/gc/docker-compose.yml exec -T db \
    pg_isready -U gc -d ground_control'

# 6b. Spring Boot health.
curl -sf http://gc-dev:8000/actuator/health | jq '.status'
# Expect: "UP"

# 6c. Data reachable via the API.
curl -s 'http://gc-dev:8000/api/v1/analysis/dashboard-stats?project=ground-control' | jq '.'
# Expect non-zero requirement / link counts that match your expectations
# for the restored point in time.

# 6d. Next scheduled backup succeeds.
ssh gc-dev /opt/gc/backup.sh && ssh gc-dev 'ls -lht /data/backups/ | head'
```

If any check fails, loop back to § 3 and pick a different source of
truth (an earlier dump or snapshot).

---

## 7. Rotating credentials

The instance reads the database password and any other SecureString
values from SSM at boot via `/opt/gc/refresh-env.sh`. Rotating is a
three-step process:

1. Write the new value to SSM:

    ```bash
    aws ssm put-parameter \
      --profile catalyst-dev --region us-east-2 \
      --name /gc/dev/db_password \
      --type SecureString --overwrite \
      --value "$(openssl rand -base64 24)"
    ```

2. Refresh the running instance's environment and restart the affected
   service:

    ```bash
    ssh gc-dev /opt/gc/refresh-env.sh
    ssh gc-dev 'cd /opt/gc && docker compose up -d'
    ```

3. Wait for the next scheduled backup (or force one with
   `ssh gc-dev /opt/gc/backup.sh`) and confirm the dump completes and
   lands in S3:

    ```bash
    aws s3 ls s3://groundcontrol-backups-catalyst-dev/pg-dumps/ \
      --profile catalyst-dev | sort | tail -3
    ssh gc-dev tail -30 /var/log/gc-backup.log
    ```

For IAM / ECR / Tailscale rotation see
`deploy/terraform/modules/secrets/` and
`deploy/terraform/modules/compute/main.tf`.

---

## 8. Escalation

- Repository: https://github.com/KeplerOps/Ground-Control
- Primary maintainer: owner of the `catalyst-dev` AWS account.
- File issues against this repository with the `ops` label if this
  runbook was wrong or incomplete.

---

## Appendix A. Where the tooling lives

| Path | Purpose |
|------|---------|
| `/opt/gc/backup.sh` | Nightly + midday + evening pg_dump, uploads to S3, local rotation |
| `/opt/gc/restore.sh` | `--list | --from-file | --from-s3` restore-in-place with safety-backup + confirm prompt |
| `/opt/gc/test-restore.sh` | Verification loop run daily by cron (05:00 UTC); also runs from `scripts/test-backup-restore-locally.sh` |
| `/var/log/gc-backup.log` | Output of scheduled backups |
| `/var/log/gc-restore-test.log` | Output of the daily verification cron |
| `/etc/cron.d/gc-backup` | Cron declaration for scheduled backups |
| `/etc/cron.d/gc-restore-test` | Cron declaration for daily verification |
| `/etc/cron.d/gc-watchdog` | 5-minute health-check watchdog (restarts backend on failure) |
| `deploy/terraform/modules/compute/user-data.sh.tftpl` | Source of truth for every `/opt/gc/*` script (written at first boot) |
| `deploy/scripts/*.sh` | Repo-side copies used for local testing and review |
| `scripts/test-backup-restore-locally.sh` | Self-contained local end-to-end test of the restore loop |
| `scripts/assert-backup-policy.sh` | Pre-commit / `make policy` guardrail preventing drift away from GC-P021 defaults |
