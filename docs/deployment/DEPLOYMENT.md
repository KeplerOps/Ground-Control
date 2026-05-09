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
| `GC_SECURITY_ENABLED` | `true` | Master switch for the API access control layer (ADR-026, GC-P011). The `dev` and `test` profiles override this to `false`. |
| `GC_SECURITY_OPENAPI_PUBLIC` | `false` | When `true`, `/api/openapi.json` and `/api/docs/**` are reachable anonymously. Otherwise authenticated. |
| `GROUNDCONTROL_SECURITY_CREDENTIALS_<N>_PRINCIPAL_NAME` | _(unset)_ | Principal name written to audit fields when this token authenticates. |
| `GROUNDCONTROL_SECURITY_CREDENTIALS_<N>_TOKEN` | _(unset)_ | Bearer token presented as `Authorization: Bearer <token>`. |
| `GROUNDCONTROL_SECURITY_CREDENTIALS_<N>_ROLE` | _(unset)_ | `USER` or `ADMIN`. Admin roles are required for `/api/v1/admin/**`, `/api/v1/embeddings/**`, `/api/v1/analysis/sweep/**`, `/api/v1/pack-registry/**`. |
| `GROUNDCONTROL_SECURITY_IP_ALLOWLIST_<N>` | _(unset)_ | Optional CIDR. Empty list = no IP gating. `X-Forwarded-For` is NOT honored — proxies must terminate the IP gate or set the source IP via `RemoteIpFilter`. |

### Authentication & Network Access (ADR-026)

Production deployments MUST configure `groundcontrol.security.credentials`
(or supply equivalent `GROUNDCONTROL_SECURITY_CREDENTIALS_*` env vars).
With `GC_SECURITY_ENABLED=true` and an empty credential list, every
authenticated route returns 401.

Authority matrix:

| Path | Required authority |
|------|--------------------|
| `/actuator/health`, `/actuator/info` | anonymous |
| `/error` | anonymous |
| `/api/openapi.json`, `/api/docs/**`, `/v3/api-docs/**`, `/swagger-ui/**` | gated by `GC_SECURITY_OPENAPI_PUBLIC` |
| `/api/v1/admin/**` | `ROLE_ADMIN` |
| `/api/v1/embeddings/**` | `ROLE_ADMIN` |
| `/api/v1/analysis/sweep/**` | `ROLE_ADMIN` |
| `/api/v1/pack-registry/**` | `ROLE_ADMIN` |
| `/api/v1/trust-policies/**` | `ROLE_ADMIN` |
| `/api/v1/pack-install-records/**` | `ROLE_ADMIN` |
| Other `/api/v1/**` | authenticated (USER or ADMIN) |
| SPA shell, static assets, SPA client routes (GET only, non-API non-actuator) | anonymous |
| Anything else | denyAll |

Clients send `Authorization: Bearer <token>` on every request. Tokens are
compared with `MessageDigest.isEqual` for constant-time compare; rotation
is a config edit + restart.

Migrating from pre-#243 deployments:

- `ground-control.pack-registry.security.admin-credentials` is removed.
  Move admin entries into `groundcontrol.security.credentials` with
  `role: ADMIN`. The pack-signing block (`trusted-signers`) is unchanged.
- The `X-Actor` header is no longer a self-service identity claim when
  security is enabled; the authenticated principal name is used for audit.

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

## Hetzner Deployment (`red-dragon` + Tailscale)

Ground Control runs on `red-dragon` (Hetzner dedicated, AMD Ryzen 7 3700X / 128 GB / 2× 1 TB NVMe), accessible only via Tailscale. The full decision record is [ADR-030](../../architecture/adrs/030-on-prem-hetzner-deployment.md). [ADR-018](../../architecture/adrs/018-aws-ec2-deployment.md) is preserved as the historical AWS predecessor.

### Architecture

- **Host**: `red-dragon` (single tailnet-resident host, Ubuntu, Docker Engine 29.x, Compose v5.x).
- **Access**: Tailscale only. sshd binds to `100.98.28.66:22` (the tailnet IP) — no public ingress. Application reachable on tailnet at `http://red-dragon:8000`.
- **Storage**: `/data/postgres` (bind-mounted into the db container) and `/data/backups` (pg_dump artifacts). Both on red-dragon's main NVMe.
- **Image registry**: GHCR (`ghcr.io/keplerops/ground-control`). Pulled by `docker compose pull` on each deploy.
- **Backups**: 3×/day `pg_dump` cron to `/data/backups/`, 30-day local retention. Off-box copy via rsync over the tailnet to `aurora`. Policy is GC-P021 / [ADR-025](../../architecture/adrs/025-backup-policy.md).
- **Cost**: $0 marginal (red-dragon is paid for unrelated reasons).

### `/opt/gc/` layout

The compose stack on red-dragon lives at `/opt/gc/`:

| Path | Owner | Purpose |
|------|-------|---------|
| `/opt/gc/docker-compose.yml` | `atomik` | Compose stack (db + backend). Mirrors `deploy/docker/docker-compose.prod.yml` in this repo. |
| `/opt/gc/.env` | `atomik` (mode 600) | Environment file consumed by compose. Carries DB credentials, GHCR image reference, embedding API keys. Not in git. |
| `/opt/gc/deploy.sh` | `root` (mode 755) | Forced-command target invoked over SSH by the `gc-deploy` user. Pulls the latest image, brings the stack up, verifies `/actuator/health`. |
| `/data/postgres/` | `999:999` | Postgres data directory (bind-mount). |
| `/data/backups/` | `atomik` | pg_dump artifacts. |

### Deploy contract

#### Automatic

Every push to `main` that passes CI + smoke triggers the `deploy` job in `.github/workflows/ci.yml`. The job runs on a fabricator-managed runner (which joins the tailnet at first boot via [`KeplerOps/fabricator` PR #14](https://github.com/KeplerOps/fabricator/pull/14)) and SSHes `gc-deploy@red-dragon`. The `gc-deploy` user's `authorized_keys` carries a single forced-command entry:

```
command="/opt/gc/deploy.sh",restrict <ed25519-pubkey>
```

`restrict` disables PTY, port forwarding, X11, agent forwarding, user-rc — the deploy key cannot do anything except run the deploy script. SSH exit code is the deploy script's exit code.

The two GitHub repo secrets that drive this:

- `RED_DRAGON_DEPLOY_KEY` — the ed25519 private key for `gc-deploy`.
- `RED_DRAGON_KNOWN_HOSTS` — host-key fingerprints for `red-dragon` (`StrictHostKeyChecking=yes`).

The `tag:fabricator-runner` → `tag:gc-host:tcp:22` tailnet ACL constrains the runner's reach to the deploy port. No other tailnet device is reachable from a runner.

#### Manual

From any tailnet host:

```bash
# Deploy latest image
ssh gc-deploy@red-dragon                     # forced command runs deploy.sh

# Operator-side compose ops (run as your normal tailnet identity, not gc-deploy)
ssh red-dragon 'cd /opt/gc && docker compose ps'
ssh red-dragon 'cd /opt/gc && docker compose logs --tail=200 backend'
```

### Data Migration (historical)

The cutover from gc-dev (EC2) to red-dragon was performed 2026-05-03:

```bash
# Dump on gc-dev
ssh ec2-user@gc-dev 'sudo docker compose -f /opt/gc/docker-compose.yml --env-file /opt/gc/.env \
  exec -T db pg_dump -Fc -U gc ground_control' > /data/backups/gc-cutover.dump

# Restore on red-dragon
docker cp /data/backups/gc-cutover.dump gc-db-1:/tmp/cutover.dump
docker exec gc-db-1 pg_restore -U gc -d ground_control --clean --if-exists --no-owner --no-acl -j 4 /tmp/cutover.dump
```

Apache AGE extension state (`ag_graph`, `ag_label`) emits ignorable duplicate-key errors during restore — those tables are pre-populated by the AGE extension at db init. Application data restores cleanly.

### Backup and Recovery

The legacy AWS path (DLM snapshots + S3 dumps) is gone. The on-prem path is:

| Layer | Mechanism | Cadence | Retention |
|-------|-----------|---------|-----------|
| Local logical | `pg_dump -Fc` from inside the db container → `/data/backups/` | 3×/day (03/11/19 UTC) | 30 days local |
| Off-box logical | `rsync` over tailnet to `aurora:/var/backups/groundcontrol/` | After each pg_dump | per-aurora retention policy |
| Restore drill | `pg_restore` of the newest dump into a throwaway db | Weekly | log retained 30 days |

GC-P021 requires backup ≥ 3×/day with off-box durability. The `assert-backup-policy.sh` gate (pre-commit / `make policy`) enforces the policy floor on whatever values the repo declares; lowering cadence below 3×/day will fail the gate.

The operator runbook is [docs/operations/backup-restore.md](../operations/backup-restore.md). Manual ops:

```bash
ssh red-dragon /opt/gc/backup.sh                  # ad-hoc backup
ssh red-dragon /opt/gc/restore.sh --list          # enumerate
ssh red-dragon /opt/gc/restore.sh /data/backups/<name>.dump
ssh red-dragon /opt/gc/test-restore.sh            # verify newest dump against a throwaway db
```

### Monitoring

- **Container health** — `docker compose ps` on `/opt/gc/` shows `(healthy)` for both `db` (`pg_isready`) and `backend` (`/actuator/health`).
- **Logs** — `docker compose logs backend` / `db`. Compose's default log driver retains in-memory; for persistence, the host's journald captures the docker daemon output.
- **Restart policy** — `restart: unless-stopped` on both services covers the container-crash case. There is no separate watchdog cron; the legacy ADR-018 watchdog was AWS-specific and has not been ported. If health degrades without the container exiting, the next deploy's health gate or operator inspection will catch it.

### Initial host setup (one-time, historical)

The setup performed on red-dragon during cutover, captured for disaster-recovery reproduction:

```bash
# Directory layout
sudo mkdir -p /opt/gc /data/postgres /data/backups
sudo chown -R 999:999 /data/postgres
sudo chown atomik:atomik /opt/gc /data/backups

# Compose + env (env mirrors gc-dev's, but GC_IMAGE points at GHCR)
sudo cp deploy/docker/docker-compose.prod.yml /opt/gc/docker-compose.yml
sudo install -o atomik -g atomik -m 600 /dev/stdin /opt/gc/.env <<'EOF'
GC_DATABASE_URL=jdbc:postgresql://db:5432/ground_control
GC_DATABASE_USER=gc
GC_DATABASE_PASSWORD=...
GC_SERVER_PORT=8000
GC_CACHE_TYPE=none
JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m
POSTGRES_DB=ground_control
POSTGRES_USER=gc
POSTGRES_PASSWORD=...
GC_IMAGE=ghcr.io/keplerops/ground-control:latest
GC_EMBEDDING_PROVIDER=openai
GC_EMBEDDING_API_KEY=...
EOF

# Deploy user (CI's SSH target, forced-command only)
sudo useradd -m -s /bin/bash gc-deploy
sudo usermod -aG docker gc-deploy
sudo install -d -o gc-deploy -g gc-deploy -m 700 /home/gc-deploy/.ssh
sudo install -o gc-deploy -g gc-deploy -m 600 /dev/stdin /home/gc-deploy/.ssh/authorized_keys <<EOF
command="/opt/gc/deploy.sh",restrict <ed25519-pubkey>
EOF

# Deploy script
sudo install -o root -g root -m 755 /dev/stdin /opt/gc/deploy.sh <<'SH'
#!/bin/bash
set -euo pipefail
cd /opt/gc
docker compose --env-file .env pull
docker compose --env-file .env up -d
for i in $(seq 1 30); do
  if curl -sf http://localhost:8000/actuator/health 2>/dev/null | grep -q '"UP"'; then
    echo "Deploy complete - application is UP"; exit 0
  fi; sleep 2
done
echo "ERROR: health check did not pass within 60s"
docker compose --env-file .env logs --tail=50 backend
exit 1
SH

# First start
gh auth token | docker login ghcr.io -u <github-user> --password-stdin
cd /opt/gc && docker compose --env-file .env up -d
```
