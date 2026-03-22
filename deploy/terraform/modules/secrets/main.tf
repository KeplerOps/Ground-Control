# -----------------------------------------------------------------------------
# Secrets Module — SSM parameters for EC2 deployment
# -----------------------------------------------------------------------------
# These parameters are created with placeholder values and must be populated
# manually via AWS CLI before the EC2 instance is launched:
#
#   aws ssm put-parameter --name "/gc/dev/spring.datasource.password" \
#     --type SecureString --value "YOUR_PASSWORD" --overwrite
#
#   aws ssm put-parameter --name "/gc/dev/groundcontrol.github.token" \
#     --type SecureString --value "ghp_..." --overwrite
#
# Parameter names match Spring property names so Spring Cloud AWS Parameter
# Store can resolve them directly (no refresh-env.sh needed).

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
