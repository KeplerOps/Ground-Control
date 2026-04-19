# Ground Control — Deployment

## Local Development

### Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker Engine 24+ and Docker Compose v2
- Gradle wrapper included (`backend/gradlew`) — no global Gradle install needed

### Setup

```bash
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control
cp .env.example .env
make up                            # Start PostgreSQL + AGE
make rapid                         # Format + compile (~1s with warm daemon)
make dev                           # Spring Boot dev server on :8000
```

Flyway migrations run automatically on application startup — there is no separate migration step.

### Docker Compose Services

The `docker-compose.yml` in the project root runs infrastructure only. Spring Boot runs on the host.

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `db` | `apache/age:release_PG16_1.6.0` | 5432 | Primary database (PostgreSQL 16 + Apache AGE 1.6.0) |
| `redis` | `redis:7` | 6379 | Available for future use (not used by the application currently) |

PostgreSQL data persists in the `gc-postgres-data` named volume.

### Environment Variables

All settings use the `GC_` prefix. See `.env.example`.

| Variable | Default | Description |
|----------|---------|-------------|
| `GC_DATABASE_URL` | `jdbc:postgresql://localhost:5432/ground_control` | JDBC connection URL |
| `GC_DATABASE_USER` | `gc` | Database username |
| `GC_DATABASE_PASSWORD` | `gc` | Database password |
| `GC_REDIS_URL` | `redis://localhost:6379` | Redis connection URL (unused by app currently) |
| `GC_SERVER_PORT` | `8000` | HTTP server port |
| `GC_AGE_ENABLED` | `false` | Enable Apache AGE graph queries |
| `GC_SWEEP_ENABLED` | `false` | Enable scheduled analysis sweeps |
| `GC_SWEEP_CRON` | `0 0 6 * * *` | Cron expression for sweep schedule |
| `GC_EMBEDDING_PROVIDER` | `none` | Embedding provider (`openai` or `none`) |
| `GC_EMBEDDING_API_KEY` | _(empty)_ | API key for the embedding provider |
| `GC_EMBEDDING_API_URL` | `https://api.openai.com/v1` | Embedding API base URL |
| `GC_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model identifier |
| `GC_EMBEDDING_DIMENSIONS` | `1536` | Embedding vector dimensions |
| `GC_EMBEDDING_BATCH_SIZE` | `100` | Max texts per batch embedding request |
| `GC_EMBEDDING_SIMILARITY_THRESHOLD` | `0.85` | Default cosine similarity threshold |

### Makefile Targets

| Target | Description |
|--------|-------------|
| `make rapid` | Format + compile, no tests or static analysis (~1s warm) |
| `make build` | Build the project (no tests) |
| `make test` | Run unit tests (no static analysis) |
| `make test-cov` | Run tests with JaCoCo coverage report |
| `make format` | Format code with Spotless |
| `make lint` | Check formatting (Spotless) |
| `make check` | Full build + tests + static analysis + coverage (CI-equivalent) |
| `make integration` | Integration tests (Testcontainers) |
| `make verify` | Full verification: check + integration tests + OpenJML ESC |
| `make dev` | Start Spring Boot dev server on :8000 |
| `make up` | `docker compose up -d` |
| `make down` | `docker compose down` |
| `make docker-build` | Build production Docker image |
| `make smoke` | Build Docker image and verify Flyway + health endpoint |
| `make clean` | Remove build artifacts |

### Production Docker Image

The backend ships as a multi-stage Docker image (`backend/Dockerfile`):

- **Builder stage**: Eclipse Temurin JDK 21 Alpine, builds fat JAR with Gradle
- **Runtime stage**: Eclipse Temurin JRE 21 Alpine, runs as non-root user `gc`
- **Entrypoint**: `java -jar app.jar` on port 8000

#### Build locally

```bash
make docker-build
# or directly:
docker build -f backend/Dockerfile -t ghcr.io/keplerops/ground-control:latest .
```

#### Run locally

```bash
docker run --rm -p 8000:8000 \
  -e GC_DATABASE_URL=jdbc:postgresql://host:5432/ground_control \
  -e GC_DATABASE_USER=gc \
  -e GC_DATABASE_PASSWORD=gc \
  ghcr.io/keplerops/ground-control:latest
```

Flyway migrations run automatically on startup — no separate migration step needed.

#### Smoke test

```bash
make smoke
```

Builds the Docker image, starts a fresh PostgreSQL 16 container, runs the app against it, and verifies Flyway migrations apply and the health endpoint returns UP.

#### CI/CD

The `docker.yml` GitHub Actions workflow automatically builds and pushes to GHCR on:
- Push to `main` or `dev`
- Semver tags (`v*`)

CI (build, test, integration, verify) must pass before the image is built.

### Resetting

```bash
make down              # stop services, keep data
docker compose down -v # stop services, delete all data
```

---

## AWS Deployment (EC2 + Tailscale)

Ground Control runs on a single EC2 instance in the `catalyst-dev` AWS account, accessible only via Tailscale. See [ADR-018](../../architecture/adrs/018-aws-ec2-deployment.md) for the full decision record.

### Architecture

- **Instance**: `t3a.small` (2 vCPU, 2 GB RAM) running Amazon Linux 2023
- **Access**: Tailscale only — zero public ingress. `http://gc-dev:8000` via MagicDNS
- **Storage**: Root EBS (8 GB gp3) + data EBS (20 GB gp3, encrypted) at `/data`
- **Backup**: EBS snapshots (daily, 7-day retention) + pg_dump to S3 (3×/day, 30-day retention) — see [ADR-025](../../architecture/adrs/025-backup-policy.md) for policy rationale (GC-P021)
- **Cost**: ~$17/mo on-demand

### Prerequisites

1. AWS CLI configured with `catalyst-dev` profile
2. Tailscale account with a reusable auth key
3. Terraform >= 1.9

### Initial Setup

#### 1. Store secrets in SSM

Before running Terraform, manually store secrets in SSM Parameter Store:

```bash
aws ssm put-parameter \
  --name "/gc/dev/tailscale_auth_key" \
  --type SecureString \
  --value "tskey-auth-..." \
  --profile catalyst-dev

aws ssm put-parameter \
  --name "/gc/dev/db_password" \
  --type SecureString \
  --value "$(openssl rand -base64 24)" \
  --profile catalyst-dev
```

#### 2. Bootstrap (first time only)

```bash
cd deploy/terraform/bootstrap
terraform init
terraform apply
```

This creates the S3 state backend, DynamoDB lock table, and GitHub Actions OIDC role.

#### 3. Deploy infrastructure

```bash
cd deploy/terraform/environments/dev
terraform init
terraform apply
```

This creates the EC2 instance, security group, EBS data volume, S3 backup bucket, and DLM snapshot policy. Cloud-init handles Docker/Tailscale/compose setup automatically.

#### 4. Verify

```bash
# Tailscale shows the instance
tailscale status | grep gc-dev

# SSH works via Tailscale
ssh gc-dev

# Application is healthy
curl http://gc-dev:8000/actuator/health

# Dashboard data available
curl http://gc-dev:8000/api/v1/analysis/dashboard-stats?project=ground-control
```

### Deploying Updates

**Automatic:** Every push to `main` that passes CI + smoke test triggers a deploy via SSM `SendCommand`. The `deploy` job in `ci.yml` finds the EC2 instance by tag, runs `/opt/gc/deploy.sh`, and streams the result.

**Manual** (if needed):

```bash
# Deploy latest image
ssh gc-dev /opt/gc/deploy.sh

# Deploy a specific tag
ssh gc-dev /opt/gc/deploy.sh v0.58.0

# Or use the Makefile target
make deploy
```

### Data Migration

To migrate data from local to EC2:

```bash
# Dump local database
docker compose exec db pg_dump -Fc -U gc ground_control > gc-local.dump

# Copy to EC2
scp gc-local.dump gc-dev:/data/backups/

# Restore on EC2
ssh gc-dev 'docker compose -f /opt/gc/docker-compose.yml exec -T db \
  pg_restore -U gc -d ground_control --clean --if-exists < /data/backups/gc-local.dump'
```

### Backup and Recovery

Ground Control uses two complementary backup strategies: **logical backups** (pg_dump to S3) for portable, table-level restore and **block-level backups** (EBS snapshots) for fast full-volume recovery. Both run automatically and are configurable via Terraform.

The authoritative operator runbook is **[docs/operations/backup-restore.md](../operations/backup-restore.md)** — it walks a cold operator through every recovery scenario without assuming prior exposure to the stack. The ADR behind the current policy is [ADR-025](../../architecture/adrs/025-backup-policy.md).

#### Backup Configuration

| Parameter | Default | Terraform Variable | Description |
|-----------|---------|-------------------|-------------|
| Backup schedule | `0 3,11,19 * * *` (03:00 / 11:00 / 19:00 UTC, 3×/day) | `backup_cron` | Cron expression for pg_dump frequency. GC-P021 requires ≥ 3×/day; do not lower. |
| S3 retention | 30 days | `s3_retention_days` | How long S3 dumps are kept |
| Local dump retention | 4 files | `local_retention_count` | Number of pg_dump files kept on EC2. GC-P021 requires ≥ 24 h; at 3×/day this means ≥ 4 files. |
| EBS snapshot retention | 7 snapshots | `snapshot_retention_count` | Number of daily EBS snapshots kept |
| EBS snapshot schedule | Daily 04:00 UTC | DLM policy (fixed) | Block-level volume snapshots |
| Restore-test schedule | Daily 05:00 UTC | fixed in `user-data.sh.tftpl` | Automated verification of the newest dump |

To change backup frequency, set the Terraform variable and redeploy.
Any override must remain ≥ 3×/day to stay GC-P021 compliant — the
`scripts/assert-backup-policy.sh` gate (run by pre-commit and
`make policy`) will fail the build otherwise.

#### Point-in-Time Restore

Point-in-time restore granularity equals the backup frequency. Each pg_dump is a consistent database snapshot at the moment it was taken. To restore to a specific point in time, select the backup with the closest timestamp before your target time. The default `0 3,11,19 * * *` schedule gives an RPO of approximately 8 hours.

#### Manual Backup / Restore / Verification

Quick reference (full procedures live in the operator runbook):

```bash
ssh gc-dev /opt/gc/backup.sh                                  # ad-hoc backup
ssh gc-dev /opt/gc/restore.sh --list                          # enumerate backups
ssh gc-dev /opt/gc/restore.sh --from-s3 pg-dumps/gc-...dump   # restore
ssh gc-dev /opt/gc/test-restore.sh                            # verify newest dump
ssh gc-dev cat /var/log/gc-restore-test.log                   # daily verification log
scripts/test-backup-restore-locally.sh                        # self-contained local loop
```

For restore-from-EBS-snapshot, credential rotation, full-loss rebuild,
or AGE graph rematerialization, follow the operator runbook rather than
ad-hoc commands.

**Automated verification checks** (executed daily at 05:00 UTC):

- PostgreSQL responds to `pg_isready`
- Public tables exist in the restored database
- `flyway_schema_history` contains migrations, including V010 (`create_age_graph`)
- The Apache AGE extension is installed
- Core Ground Control tables exist (`project`, `requirement`, `requirement_relation`, `traceability_link`, `document`, `section`, `threat_model`)
- `create_graph('requirements_verify')` succeeds — proves AGE is operationally usable against restored data

### Monitoring

- **Health checks**: Docker healthcheck on both `db` (`pg_isready`) and `backend` (`/actuator/health`)
- **Watchdog**: Cron runs every 5 min, restarts backend on health failure. Logs to `/var/log/gc-watchdog.log`
- **Init log**: `/var/log/gc-init.log` — cloud-init output for troubleshooting first boot
- **Backup log**: `/var/log/gc-backup.log` — backup cron output
- **Restore test log**: `/var/log/gc-restore-test.log` — weekly restore test output

### Terraform Modules

| Module | Purpose |
|--------|---------|
| `modules/compute/` | EC2 instance, IAM instance profile, EBS data volume, cloud-init |
| `modules/networking/` | Zero-ingress security group (Tailscale only) |
| `modules/backup/` | S3 backup bucket, DLM EBS snapshot policy |
| `modules/secrets/` | SSM parameters (Tailscale key, DB password) |
