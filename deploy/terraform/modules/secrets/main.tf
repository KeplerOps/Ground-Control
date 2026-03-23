# -----------------------------------------------------------------------------
# Secrets Module — SSM parameters for EC2 deployment
# -----------------------------------------------------------------------------
# Terraform creates these parameters with placeholder values. The CI/CD
# pipeline (GitHub Actions deploy job) populates them from GitHub secrets
# on every deploy. Parameter names match Spring property names so Spring
# Cloud AWS Parameter Store resolves them directly at app startup.
#
# Migration note: renaming an SSM parameter causes Terraform to destroy
# the old one and create a new one with the placeholder value. Run a
# deploy (or manually sync secrets) immediately after terraform apply.

resource "aws_ssm_parameter" "tailscale_auth_key" {
  name  = "${var.parameter_prefix}/tailscale_auth_key"
  type  = "SecureString"
  value = var.tailscale_auth_key

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_password" {
  name  = "${var.parameter_prefix}/spring.datasource.password"
  type  = "SecureString"
  value = var.db_password

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "embedding_api_key" {
  name  = "${var.parameter_prefix}/groundcontrol.embedding.api-key"
  type  = "SecureString"
  value = var.embedding_api_key

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "github_token" {
  name  = "${var.parameter_prefix}/groundcontrol.github.token"
  type  = "SecureString"
  value = var.github_token

  lifecycle {
    ignore_changes = [value]
  }
}
