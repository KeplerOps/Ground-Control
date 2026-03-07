# Ground Control — Deployment & SSO Guide

**Version:** 1.0.0
**Date:** 2026-03-07

---

## 1. Deployment Options Overview

| Option | Best For | Complexity | Scaling |
|---|---|---|---|
| **Docker Compose** | Small teams, evaluation, development | Low | Vertical only |
| **Kubernetes (Helm)** | Production, multi-tenant, enterprise | Medium | Horizontal |
| **Cloud Managed** | Teams preferring managed infrastructure | Medium | Auto-scaling |
| **Single Binary** | Minimal deployments, air-gapped networks | Lowest | Vertical only |

---

## 2. Docker Compose Deployment

### 2.1 Prerequisites

- Docker Engine 24+ and Docker Compose v2
- 4 GB RAM minimum (8 GB recommended)
- 20 GB disk (more for artifact storage)
- Domain name with DNS pointing to the host (for TLS)

### 2.2 Quick Start

```bash
# Clone the repository
git clone https://github.com/KeplerOps/Ground-Control.git
cd Ground-Control

# Copy environment template
cp .env.example .env

# Edit configuration
vi .env

# Start all services
docker compose up -d

# Run database migrations
docker compose exec app gc-migrate upgrade

# Create initial admin user
docker compose exec app gc-admin create-user \
  --email admin@example.com \
  --name "Admin" \
  --role admin
```

### 2.3 Docker Compose Architecture

```yaml
# docker-compose.yml
services:
  app:
    image: ghcr.io/keplerops/ground-control:latest
    ports:
      - "8000:8000"
    environment:
      DATABASE_URL: postgresql+asyncpg://gc:${DB_PASSWORD}@db:5432/groundcontrol
      REDIS_URL: redis://redis:6379/0
      S3_ENDPOINT: http://minio:9000
      S3_BUCKET: gc-artifacts
      S3_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      S3_SECRET_KEY: ${MINIO_SECRET_KEY}
      SEARCH_URL: http://meilisearch:7700
      SECRET_KEY: ${SECRET_KEY}
      ALLOWED_ORIGINS: https://gc.example.com
    depends_on:
      - db
      - redis
      - minio
      - meilisearch

  worker:
    image: ghcr.io/keplerops/ground-control:latest
    command: gc-worker
    environment:
      # Same as app
    depends_on:
      - db
      - redis

  db:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data
    environment:
      POSTGRES_DB: groundcontrol
      POSTGRES_USER: gc
      POSTGRES_PASSWORD: ${DB_PASSWORD}

  redis:
    image: redis:7-alpine
    volumes:
      - redisdata:/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    volumes:
      - miniodata:/data
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}

  meilisearch:
    image: getmeili/meilisearch:latest
    volumes:
      - searchdata:/meili_data
    environment:
      MEILI_MASTER_KEY: ${SEARCH_KEY}

  caddy:
    image: caddy:2-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddydata:/data

volumes:
  pgdata:
  redisdata:
  miniodata:
  searchdata:
  caddydata:
```

### 2.4 Caddyfile (Reverse Proxy with Auto-TLS)

```
gc.example.com {
    reverse_proxy app:8000
}
```

### 2.5 Environment Variables

```bash
# .env.example

# --- Core ---
SECRET_KEY=change-me-to-a-random-64-char-string
ALLOWED_ORIGINS=https://gc.example.com
LOG_LEVEL=info

# --- Database ---
DB_PASSWORD=change-me

# --- Object Storage ---
MINIO_ACCESS_KEY=gc-access-key
MINIO_SECRET_KEY=change-me

# --- Search ---
SEARCH_KEY=change-me

# --- SSO (optional) ---
SSO_PROVIDER=                # saml or oidc
SAML_IDP_METADATA_URL=
SAML_SP_ENTITY_ID=
OIDC_ISSUER=
OIDC_CLIENT_ID=
OIDC_CLIENT_SECRET=

# --- SCIM (optional) ---
SCIM_ENABLED=false
SCIM_BEARER_TOKEN=

# --- Email (optional) ---
SMTP_HOST=
SMTP_PORT=587
SMTP_USER=
SMTP_PASSWORD=
SMTP_FROM=noreply@example.com

# --- Encryption ---
ARTIFACT_ENCRYPTION_KEY=     # 32-byte base64 key for artifact encryption
```

---

## 3. Kubernetes Deployment (Helm)

### 3.1 Prerequisites

- Kubernetes 1.28+
- Helm 3.14+
- kubectl configured for target cluster
- Ingress controller (nginx-ingress or traefik)
- cert-manager (for TLS certificates) — optional if using cloud LB

### 3.2 Install

```bash
# Add Helm repo
helm repo add ground-control https://keplerops.github.io/ground-control-charts
helm repo update

# Create namespace
kubectl create namespace ground-control

# Create secrets
kubectl create secret generic gc-secrets \
  --namespace ground-control \
  --from-literal=secret-key=$(openssl rand -hex 32) \
  --from-literal=db-password=$(openssl rand -hex 16) \
  --from-literal=minio-secret-key=$(openssl rand -hex 16) \
  --from-literal=search-key=$(openssl rand -hex 16)

# Install with custom values
helm install ground-control ground-control/ground-control \
  --namespace ground-control \
  --values values.yaml
```

### 3.3 Helm Values

```yaml
# values.yaml

replicaCount:
  app: 3
  worker: 2

image:
  repository: ghcr.io/keplerops/ground-control
  tag: "1.0.0"

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: gc.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: gc-tls
      hosts:
        - gc.example.com

postgresql:
  enabled: true          # Use bundled PostgreSQL
  auth:
    existingSecret: gc-secrets
    secretKeys:
      adminPasswordKey: db-password
  primary:
    persistence:
      size: 50Gi

redis:
  enabled: true
  architecture: standalone
  auth:
    enabled: false

minio:
  enabled: true
  auth:
    existingSecret: gc-secrets
  persistence:
    size: 100Gi

meilisearch:
  enabled: true
  auth:
    existingSecret: gc-secrets
  persistence:
    size: 10Gi

# External database (instead of bundled)
# externalDatabase:
#   host: my-rds-instance.region.rds.amazonaws.com
#   port: 5432
#   database: groundcontrol
#   existingSecret: gc-db-secret

resources:
  app:
    requests:
      cpu: 500m
      memory: 512Mi
    limits:
      cpu: 2000m
      memory: 2Gi
  worker:
    requests:
      cpu: 250m
      memory: 256Mi
    limits:
      cpu: 1000m
      memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

persistence:
  artifacts:
    storageClass: ""       # Use default
    size: 100Gi

monitoring:
  enabled: true
  serviceMonitor: true     # Prometheus ServiceMonitor
```

### 3.4 Kubernetes Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  Namespace: ground-control                                        │
│                                                                  │
│  ┌─────────────┐     ┌───────────────────┐     ┌─────────────┐ │
│  │  Ingress     │────▶│  Service: gc-app  │────▶│ Deployment: │ │
│  │  (TLS term.) │     │  (ClusterIP)      │     │ gc-app      │ │
│  └─────────────┘     └───────────────────┘     │ (3 replicas) │ │
│                                                 └─────────────┘ │
│                                                                  │
│  ┌───────────────────┐     ┌──────────────────────────────────┐ │
│  │ Deployment:        │     │ StatefulSets:                    │ │
│  │ gc-worker          │     │ ├── PostgreSQL (or external RDS) │ │
│  │ (2 replicas)       │     │ ├── Redis                       │ │
│  └───────────────────┘     │ ├── MinIO (or external S3)      │ │
│                             │ └── Meilisearch                 │ │
│  ┌───────────────────┐     └──────────────────────────────────┘ │
│  │ CronJob:           │                                          │
│  │ gc-scheduled-tasks │     ┌──────────────────────────────────┐ │
│  └───────────────────┘     │ ConfigMap: gc-config              │ │
│                             │ Secret: gc-secrets                │ │
│  ┌───────────────────┐     │ PVC: gc-artifacts                 │ │
│  │ Job: gc-migrate    │     └──────────────────────────────────┘ │
│  │ (runs on upgrade)  │                                          │
│  └───────────────────┘     ┌──────────────────────────────────┐ │
│                             │ NetworkPolicy: restrict inter-pod │ │
│                             │ PodDisruptionBudget: min 1 avail │ │
│                             └──────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Cloud-Managed Deployment

### 4.1 AWS Reference Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  AWS Region                                                   │
│                                                              │
│  ┌──────────┐     ┌──────────────┐     ┌──────────────────┐ │
│  │   ALB    │────▶│  ECS Fargate  │────▶│  RDS PostgreSQL  │ │
│  │  (TLS)   │     │  (API + UI)   │     │  (Multi-AZ)      │ │
│  └──────────┘     └──────────────┘     └──────────────────┘ │
│                   ┌──────────────┐     ┌──────────────────┐ │
│                   │  ECS Fargate  │     │  ElastiCache     │ │
│                   │  (Workers)    │     │  Redis            │ │
│                   └──────────────┘     └──────────────────┘ │
│                   ┌──────────────┐                           │
│                   │  S3 Bucket   │     SSO: Cognito or      │
│                   │  (Artifacts) │     SAML federation      │
│                   └──────────────┘                           │
│                   ┌──────────────┐                           │
│                   │  OpenSearch  │     Monitoring:           │
│                   │  (Search)    │     CloudWatch            │
│                   └──────────────┘                           │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 Azure Reference Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Azure Region                                                 │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐│
│  │ App Gateway │─▶│ Container    │─▶│ Azure DB for         ││
│  │ (TLS/WAF)   │  │ Apps (API)   │  │ PostgreSQL           ││
│  └─────────────┘  └──────────────┘  └──────────────────────┘│
│                   ┌──────────────┐  ┌──────────────────────┐│
│                   │ Container    │  │ Azure Cache          ││
│                   │ Apps (Worker)│  │ for Redis            ││
│                   └──────────────┘  └──────────────────────┘│
│                   ┌──────────────┐                           │
│                   │ Blob Storage │  SSO: Azure AD / Entra   │
│                   │ (Artifacts)  │  ID (SAML/OIDC native)  │
│                   └──────────────┘                           │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. SSO Configuration

### 5.1 SAML 2.0

#### Ground Control SP Configuration

| Setting | Value |
|---|---|
| Entity ID | `https://gc.example.com/saml/metadata` |
| ACS URL | `https://gc.example.com/api/v1/auth/saml/acs` |
| SLO URL | `https://gc.example.com/api/v1/auth/saml/slo` |
| NameID Format | `urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress` |
| Signed Requests | Yes (RSA-SHA256) |

#### Required SAML Attributes

| Attribute | SAML Name | Required |
|---|---|---|
| Email | `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress` | Yes |
| Display Name | `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name` | Yes |
| Groups | `http://schemas.xmlsoap.org/claims/Group` | Optional (for role mapping) |

#### Configuration Steps

1. **In Ground Control Admin UI:**
   ```
   Settings → Authentication → SSO → SAML 2.0
   - IdP Metadata URL: https://login.example.com/saml/metadata
     (or upload IdP metadata XML)
   - Attribute Mapping:
     email: emailaddress
     display_name: name
     groups: Group
   ```

2. **In your IdP (e.g., Okta, Azure AD):**
   - Create a new SAML 2.0 application
   - Set ACS URL to `https://gc.example.com/api/v1/auth/saml/acs`
   - Set Entity ID to `https://gc.example.com/saml/metadata`
   - Configure attribute statements (email, name, groups)
   - Assign users/groups

3. **Test:** Click "Test SSO" in Ground Control admin → should complete round-trip.

4. **Enforce:** Enable "Require SSO" to disable local password login.

#### Okta-Specific Setup

```
1. Okta Admin → Applications → Create App Integration → SAML 2.0
2. General Settings:
   - App name: Ground Control
   - Logo: (upload)
3. SAML Settings:
   - Single sign-on URL: https://gc.example.com/api/v1/auth/saml/acs
   - Audience URI: https://gc.example.com/saml/metadata
   - Name ID format: EmailAddress
   - Attribute Statements:
     name → user.displayName
     email → user.email
   - Group Attribute Statements:
     groups → Matches regex: .*
4. Assignments: Assign users or groups
```

#### Azure AD / Entra ID — Specific Setup

```
1. Azure Portal → Enterprise Applications → New Application → Create your own
2. Single sign-on → SAML
3. Basic SAML Configuration:
   - Identifier: https://gc.example.com/saml/metadata
   - Reply URL: https://gc.example.com/api/v1/auth/saml/acs
4. Attributes & Claims:
   - email → user.mail
   - name → user.displayname
   - groups → (add group claim)
5. Assign users and groups
```

### 5.2 OpenID Connect (OIDC)

#### Configuration

```
Settings → Authentication → SSO → OpenID Connect
- Issuer URL: https://accounts.google.com
              or https://login.microsoftonline.com/{tenant}/v2.0
              or https://dev-xxxxx.okta.com/oauth2/default
- Client ID: (from IdP)
- Client Secret: (from IdP)
- Scopes: openid profile email
- Redirect URI: https://gc.example.com/api/v1/auth/oidc/callback
```

#### OIDC Flow

```
1. User clicks "Sign in with SSO"
2. Browser redirects to IdP authorization endpoint
3. User authenticates at IdP
4. IdP redirects back to Ground Control with authorization code
5. Ground Control exchanges code for tokens (server-side)
6. Ground Control validates ID token, extracts claims
7. User session created; JWT issued
```

### 5.3 SCIM 2.0 Provisioning

Ground Control exposes a SCIM 2.0 endpoint for automated user/group sync.

#### SCIM Endpoint

```
Base URL: https://gc.example.com/api/v1/scim/v2
Authentication: Bearer token

Supported resources:
- /Users        — Create, Read, Update, Delete, List
- /Groups       — Create, Read, Update, Delete, List
- /Schemas      — Discovery
- /ServiceProviderConfig — Capabilities
```

#### SCIM Setup

1. **In Ground Control:** Settings → Provisioning → SCIM
   - Enable SCIM
   - Generate bearer token
   - Copy SCIM base URL

2. **In your IdP:**
   - Okta: Applications → Your App → Provisioning → Enable SCIM
   - Azure AD: Enterprise App → Provisioning → Automatic → Enter SCIM URL + token
   - Configure attribute mapping
   - Enable: Create, Update, Deactivate

3. **Role Mapping:** Map IdP groups to Ground Control roles:
   ```
   IdP Group "GRC-Admins"     → Role: Admin
   IdP Group "IT-Audit"       → Role: Auditor
   IdP Group "Risk-Team"      → Role: Risk Manager
   IdP Group "Control-Owners" → Role: Control Owner
   ```

---

## 6. Multi-Tenancy Configuration

### 6.1 Shared Schema (Default)

Every table has a `tenant_id` column. Row-Level Security (RLS) enforces isolation.

```yaml
# values.yaml or .env
MULTI_TENANCY_MODE: shared_schema
```

### 6.2 Schema Per Tenant

Each tenant gets a dedicated PostgreSQL schema. More isolation, more overhead.

```yaml
MULTI_TENANCY_MODE: schema_per_tenant
```

### 6.3 Database Per Tenant

Each tenant gets a dedicated database. Maximum isolation for regulated environments.

```yaml
MULTI_TENANCY_MODE: database_per_tenant
TENANT_DB_TEMPLATE: postgresql+asyncpg://gc:{password}@{host}:5432/gc_{tenant_slug}
```

---

## 7. Backup & Disaster Recovery

### 7.1 Backup Strategy

| Component | Method | Frequency | Retention |
|---|---|---|---|
| PostgreSQL | pg_dump or WAL archiving | Continuous (WAL) + daily full | 30 days |
| Object Storage | S3 versioning + cross-region replication | Continuous | Per retention policy |
| Redis | RDB snapshots | Hourly | 24 hours |
| Search Index | Rebuild from PostgreSQL | On-demand | N/A (derived data) |
| Configuration | Git (values.yaml, .env) | On change | Indefinite |

### 7.2 Recovery Procedures

```bash
# Database restore from backup
pg_restore -d groundcontrol backup.dump

# Rebuild search index
docker compose exec app gc-admin reindex-search

# Verify artifact integrity
docker compose exec app gc-admin verify-artifacts --repair
```

### 7.3 Recovery Targets

| Metric | Target |
|---|---|
| RPO (Recovery Point Objective) | < 1 hour |
| RTO (Recovery Time Objective) | < 4 hours |

---

## 8. Monitoring & Observability

### 8.1 Health Endpoints

```
GET /health          → 200 if all dependencies healthy
GET /health/ready    → 200 if ready to serve traffic
GET /health/live     → 200 if process is alive
```

### 8.2 Metrics (Prometheus)

```
GET /metrics

# Key metrics:
gc_api_requests_total{method, path, status}
gc_api_request_duration_seconds{method, path}
gc_active_users_total{tenant}
gc_artifacts_stored_bytes{tenant}
gc_assessments_active_total{tenant}
gc_agent_results_total{agent_id, status}
gc_plugin_health{plugin, status}
gc_background_jobs_total{queue, status}
gc_background_job_duration_seconds{queue}
```

### 8.3 Logging

Structured JSON logs:
```json
{
  "timestamp": "2026-03-07T15:30:00Z",
  "level": "info",
  "service": "gc-app",
  "tenant_id": "...",
  "request_id": "req_abc123",
  "message": "Risk assessment completed",
  "risk_id": "...",
  "user_id": "..."
}
```

Log aggregation: Forward to Elasticsearch/Loki via stdout (container logging driver).

### 8.4 Alerting Recommendations

| Alert | Condition | Severity |
|---|---|---|
| API error rate | > 5% 5xx in 5 min | Critical |
| API latency | p95 > 1s for 5 min | Warning |
| Database connections | > 80% pool utilized | Warning |
| Disk usage | > 85% on any volume | Warning |
| Certificate expiry | < 14 days | Warning |
| Failed background jobs | > 10 failures in 1 hour | Warning |
| Plugin health check | Failed 3 consecutive times | Warning |

---

## 9. Security Hardening Checklist

- [ ] Change all default secrets in `.env` / Kubernetes secrets
- [ ] Enable TLS for all external endpoints
- [ ] Enable database encryption at rest
- [ ] Configure network policies (Kubernetes) or security groups (cloud)
- [ ] Enable audit logging and forward to SIEM
- [ ] Configure SSO and disable local password login
- [ ] Set up SCIM for automated user provisioning
- [ ] Enable MFA for any remaining local accounts
- [ ] Review and restrict API rate limits
- [ ] Configure artifact encryption keys (BYOK if required)
- [ ] Enable database connection encryption (SSL)
- [ ] Set up automated backups and test restore procedures
- [ ] Configure Content Security Policy headers
- [ ] Enable HSTS
- [ ] Review plugin permissions before enabling

---

## 10. Upgrade Procedures

### 10.1 Docker Compose

```bash
# Pull latest images
docker compose pull

# Apply database migrations
docker compose run --rm app gc-migrate upgrade

# Restart services
docker compose up -d

# Verify health
curl https://gc.example.com/health
```

### 10.2 Kubernetes (Helm)

```bash
# Update chart repo
helm repo update

# Review changes
helm diff upgrade ground-control ground-control/ground-control \
  --namespace ground-control \
  --values values.yaml

# Apply upgrade (migrations run as pre-upgrade hook)
helm upgrade ground-control ground-control/ground-control \
  --namespace ground-control \
  --values values.yaml

# Verify
kubectl rollout status deployment/gc-app -n ground-control
```

### 10.3 Rollback

```bash
# Helm rollback
helm rollback ground-control -n ground-control

# Docker Compose rollback
docker compose down
docker compose pull  # with previous image tag in docker-compose.yml
docker compose up -d
docker compose exec app gc-migrate downgrade -1
```
