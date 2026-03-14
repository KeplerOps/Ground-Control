# Phase 2: Cloud Database — Design Notes

> **Status: WITHDRAWN** — RDS decision pulled per ADR-015 (violates ADR-005's AGE commitment).
> Development defaults to local Docker Compose with `apache/age` image.

## What Was Built

- Terraform modules (`networking`, `rds`, `secrets`) in `deploy/terraform/modules/`
- Terraform environment wiring in `deploy/terraform/environments/dev/`
- Terraform bootstrap (S3 state bucket + DynamoDB lock) in `deploy/terraform/bootstrap/`
- CI terraform job (plan on PR, manual apply via workflow_dispatch)
- Makefile cloud DB targets (`cloud-db-env`, `dev-cloud`, `cloud-db-ip`)

## What Was Destroyed

- RDS instance (`groundcontrol-dev.cjmae0omm2br.us-east-2.rds.amazonaws.com`)
- Security group, parameter group, SSM parameters
- CI terraform job, path detection job, OIDC permissions
- Cloud DB Makefile targets

## Why

RDS does not support Apache AGE. ADR-005 chose AGE so graph and relational data share one database. Accepting "AGE disabled on RDS" contradicts that decision. See ADR-015 for full rationale.

## Current Data Durability Model

- Named Docker volume (`gc-postgres-data`) persists across container lifecycle
- Data is reproducible: StrictDoc import + GitHub sync (both idempotent)
- Manual `pg_dump` for snapshots when needed

## Future Cloud Path

When cloud deployment is needed, use self-managed PostgreSQL+AGE on compute that supports the extension (EC2, ECS, etc.), not a managed service that drops AGE.

## References

- [ADR-015: Cloud Database Deployment (Withdrawn)](../adrs/015-cloud-database-deployment.md)
- [ADR-005: Apache AGE](../adrs/005-apache-age-graph.md)
