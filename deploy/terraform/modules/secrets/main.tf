# -----------------------------------------------------------------------------
# Secrets Module — SSM parameters for EC2 deployment
# -----------------------------------------------------------------------------
# These parameters are created with placeholder values and must be populated
# manually via AWS CLI before the EC2 instance is launched:
#
#   aws ssm put-parameter --name "/gc/dev/tailscale_auth_key" \
#     --type SecureString --value "tskey-auth-..." --overwrite
#
#   aws ssm put-parameter --name "/gc/dev/db_password" \
#     --type SecureString --value "YOUR_PASSWORD" --overwrite

resource "aws_ssm_parameter" "tailscale_auth_key" {
  name  = "${var.parameter_prefix}/tailscale_auth_key"
  type  = "SecureString"
  value = var.tailscale_auth_key

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_password" {
  name  = "${var.parameter_prefix}/db_password"
  type  = "SecureString"
  value = var.db_password

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "embedding_api_key" {
  name  = "${var.parameter_prefix}/embedding_api_key"
  type  = "SecureString"
  value = var.embedding_api_key

  lifecycle {
    ignore_changes = [value]
  }
}
