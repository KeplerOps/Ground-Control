# Phase 2: Cloud Database — Design Notes

> **Status: DESIGN** — implementation not yet started.
> See [ADR-015](../adrs/015-cloud-database-deployment.md) for the decision record.

## Overview

Move Ground Control's PostgreSQL database from a local Docker volume to AWS RDS while keeping the application running locally. The data — requirements, relations, traceability links, audit history — is the system's source of truth and must survive machine loss.

## Topology

```
┌─────────────────────────┐         ┌──────────────────────────────┐
│   Developer Machine     │         │   AWS (catalyst-dev)         │
│                         │         │   us-east-2                  │
│  ┌───────────────────┐  │         │                              │
│  │ Spring Boot App   │  │  TLS    │  ┌────────────────────────┐  │
│  │ localhost:8000     │──┼────────┼──│ RDS PostgreSQL 16      │  │
│  │                   │  │  5432   │  │ db.t4g.micro           │  │
│  │ GC_AGE_ENABLED=   │  │         │  │ 20 GiB gp3             │  │
│  │   false           │  │         │  │ Encrypted at rest      │  │
│  └───────────────────┘  │         │  └────────┬───────────────┘  │
│                         │         │           │                  │
│  ┌───────────────────┐  │         │  ┌────────┴───────────────┐  │
│  │ Docker Compose    │  │         │  │ Security Group         │  │
│  │ (fallback)        │  │         │  │ Ingress: dev IP /32    │  │
│  │ PostgreSQL + AGE  │  │         │  │ Port: 5432/tcp         │  │
│  └───────────────────┘  │         │  └────────────────────────┘  │
│                         │         │                              │
│                         │         │  ┌────────────────────────┐  │
│  ┌───────────────────┐  │         │  │ SSM Parameter Store    │  │
│  │ AWS CLI           │──┼────────┼──│ /gc/dev/db_host        │  │
│  │ aws ssm get-param │  │         │  │ /gc/dev/db_password    │  │
│  └───────────────────┘  │         │  │ /gc/dev/db_username    │  │
│                         │         │  └────────────────────────┘  │
│                         │         │                              │
│                         │         │  ┌────────────────────────┐  │
│                         │         │  │ S3 (terraform state)   │  │
│                         │         │  │ Encrypted + Versioned  │  │
│                         │         │  │ DynamoDB lock table    │  │
│                         │         │  └────────────────────────┘  │
└─────────────────────────┘         └──────────────────────────────┘
```

## RDS Configuration

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Engine | PostgreSQL 16 | Matches local Docker version |
| Instance class | `db.t4g.micro` | 2 vCPU, 1 GiB RAM — smallest viable, ~$12/mo |
| Storage | 20 GiB gp3 | Minimum gp3, sufficient for requirements data |
| Multi-AZ | No | Single-user pre-alpha |
| Backup retention | 7 days | Automated daily backups |
| Deletion protection | Enabled | Prevent accidental deletion |
| Final snapshot | Required | Before any instance deletion |
| Maintenance window | Sun 06:00-07:00 UTC | Low-impact window |
| Auto minor upgrade | Enabled | Stay patched |
| Database name | `ground_control` | Matches local config |
| Port | 5432 | Standard PostgreSQL |

## Security Model

### Network access

Security group with a single ingress rule:

```
Ingress: TCP 5432 from <developer_ip>/32
Egress:  All (default)
```

**Why no VPN or private subnet:** This is a single-developer pre-alpha system with no customer data. The threat model is: unauthorized access to requirements metadata. The security controls (SG IP restriction + TLS + strong random password) are proportional. A VPN adds operational complexity (client config, DNS, key management) without meaningful risk reduction at this stage.

**IP update workflow:** Developer's IP changes require updating the security group. A Makefile target (`make cloud-db-ip`) automates this via `aws ec2 authorize-security-group-ingress` / `revoke-security-group-ingress`.

### Encryption

- **At rest:** RDS storage encryption using AWS-managed KMS key (AES-256). Enabled at instance creation, cannot be disabled.
- **In transit:** `rds.force_ssl=1` parameter group setting forces all connections to use TLS. Application connection string uses `sslmode=require`.

### Credential management

1. Terraform generates a random password (`random_password` resource)
2. Password stored in SSM Parameter Store as SecureString (`/gc/dev/db_password`)
3. Host and username also stored in SSM (`/gc/dev/db_host`, `/gc/dev/db_username`)
4. Developer retrieves credentials via: `aws ssm get-parameter --name /gc/dev/db_password --with-decryption`
5. Terraform state contains the password — mitigated by encrypted S3 backend

**Why SSM over Secrets Manager:** SSM Parameter Store SecureString is free (uses KMS default key). Secrets Manager costs $0.40/secret/month and adds rotation features we don't need yet.

### Application authentication

No application-level auth. The Spring Boot app runs on `localhost:8000` and is not exposed to the network. Application auth will be added when the app itself moves to cloud deployment.

## Terraform Structure

```
deploy/terraform/
├── bootstrap/
│   ├── main.tf              # S3 bucket + DynamoDB lock table
│   ├── variables.tf
│   └── outputs.tf
├── modules/
│   ├── networking/
│   │   ├── main.tf          # Security group, ingress rules
│   │   ├── variables.tf     # vpc_id, allowed_ip, port
│   │   └── outputs.tf      # security_group_id
│   ├── rds/
│   │   ├── main.tf          # RDS instance, parameter group, subnet group
│   │   ├── variables.tf     # instance_class, storage, engine, sg_id, etc.
│   │   └── outputs.tf      # endpoint, port, db_name
│   └── secrets/
│       ├── main.tf          # SSM parameters (host, username, password)
│       ├── variables.tf     # parameter values
│       └── outputs.tf      # parameter ARNs
└── environments/
    └── dev/
        ├── main.tf          # Module wiring, provider config
        ├── variables.tf     # Environment-specific variables
        ├── outputs.tf       # Useful outputs (endpoint, SSM paths)
        ├── backend.tf       # S3 backend configuration
        └── terraform.tfvars # Non-secret variable values
```

The `bootstrap/` directory is applied once manually to create the S3 bucket and DynamoDB table. All subsequent infrastructure is managed via `environments/dev/`.

## Cost Estimate

| Resource | Monthly cost |
|----------|-------------|
| RDS `db.t4g.micro` (on-demand) | ~$12.00 |
| RDS storage (20 GiB gp3) | ~$1.84 |
| RDS backup storage (within free tier) | $0.00 |
| SSM Parameter Store (standard) | $0.00 |
| S3 (terraform state, <1 MB) | ~$0.01 |
| DynamoDB (on-demand, minimal reads) | ~$0.01 |
| **Total** | **~$14/mo** |

## Local Development Workflow

### Environment variable flow

The app uses three environment variables (already defined in `application.yml`):

```
GC_DATABASE_URL=jdbc:postgresql://<rds-endpoint>:5432/ground_control?sslmode=require
GC_DATABASE_USER=gcadmin
GC_DATABASE_PASSWORD=<from-ssm>
```

Plus the AGE flag:

```
GC_AGE_ENABLED=false
```

### Makefile targets

```makefile
# Retrieve cloud DB credentials and export as env vars
cloud-db-env:
	@echo "export GC_DATABASE_URL=jdbc:postgresql://...:5432/ground_control?sslmode=require"
	@echo "export GC_DATABASE_USER=$$(aws ssm get-parameter ...)"
	@echo "export GC_DATABASE_PASSWORD=$$(aws ssm get-parameter ...)"
	@echo "export GC_AGE_ENABLED=false"

# Start app with cloud DB
dev-cloud: cloud-db-env
	cd backend && ./gradlew bootRun

# Update security group with current IP
cloud-db-ip:
	# Revoke old, authorize new
```

### Switching between local and cloud

| Mode | Command | Database |
|------|---------|----------|
| Local (Docker) | `make up && cd backend && ./gradlew bootRun` | Docker Compose PostgreSQL + AGE |
| Cloud (RDS) | `eval $(make cloud-db-env) && cd backend && ./gradlew bootRun` | RDS PostgreSQL 16 |

Local Docker Compose remains the default and works offline.

## Data Migration

Two paths:

### Path A: pg_dump/psql (preserves existing data)

```bash
# Dump from local Docker
docker exec ground-control-db pg_dump -U gc -d ground_control > gc_dump.sql

# Restore to RDS
psql "postgresql://gcadmin:<password>@<rds-endpoint>:5432/ground_control?sslmode=require" < gc_dump.sql
```

This preserves UUIDs, audit trail (`revinfo`, `*_audit` tables), and all traceability links.

### Path B: Fresh Flyway + re-import (clean slate)

```bash
# Flyway runs on first boot against empty RDS
./gradlew bootRun  # migrations V001-V010 apply

# V010 graceful fallback: AGE extension not available, EXCEPTION handler logs notice and skips

# Re-import requirements from StrictDoc
curl -X POST http://localhost:8000/api/v1/admin/import/strictdoc -F file=@requirements.sdoc

# Re-sync GitHub issues
curl -X POST "http://localhost:8000/api/v1/admin/sync/github?owner=keplerops&repo=Ground-Control"
```

Path A is preferred when existing data (especially audit history) must be preserved. Path B is acceptable for a fresh start.

## AGE Trade-off

Apache AGE is **not available on RDS** (not an supported extension). This is explicitly acceptable:

- ADR-005 documents this as a known negative consequence
- All graph analysis (`AnalysisService`) works via JPA queries on `RequirementRelation` entities
- `GraphAlgorithms` is a pure utility class with no database dependency
- `AgeGraphService` is optional, gated by `groundcontrol.age.enabled` (default: `false`)
- The local Docker Compose environment retains the AGE image for development/testing of graph features

Setting `GC_AGE_ENABLED=false` (the default) ensures no AGE-related code executes against the RDS instance.

## References

- [ADR-015: Cloud Database Deployment](../adrs/015-cloud-database-deployment.md)
- [ADR-002: PostgreSQL as Primary Database](../adrs/002-postgresql-database.md)
- [ADR-005: Apache AGE](../adrs/005-apache-age-graph.md)
- [Infrastructure Requirements](../../docs/requirements/infrastructure.sdoc)
- Spring Boot config: `backend/src/main/resources/application.yml`
- AGE migration: `backend/src/main/resources/db/migration/V010__create_age_graph.sql`
