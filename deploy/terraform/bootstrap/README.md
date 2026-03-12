# Terraform Bootstrap

One-time setup for Terraform state backend and GitHub Actions OIDC authentication.

## Prerequisites

- AWS CLI configured with credentials for the `catalyst-dev` account (516608939870)
- Terraform >= 1.9

## Usage

```bash
cd deploy/terraform/bootstrap
terraform init
terraform plan
terraform apply
```

## What it creates

- **S3 bucket** (`groundcontrol-terraform-state-catalyst-dev`): Terraform state storage with versioning, encryption (AES-256), and public access blocking
- **DynamoDB table** (`groundcontrol-terraform-lock`): State locking
- **OIDC provider**: GitHub Actions identity federation
- **IAM role** (`github-actions-terraform`): Assumed by GitHub Actions via OIDC, scoped to `KeplerOps/Ground-Control`

## Post-apply

Save these outputs for `environments/dev/backend.tf`:

```bash
terraform output state_bucket_name
terraform output lock_table_name
terraform output github_actions_role_arn
```

## Important

This configuration uses `prevent_destroy` on the S3 bucket and DynamoDB table. These resources are **not** managed by CI — they are applied manually and should never be destroyed accidentally.
