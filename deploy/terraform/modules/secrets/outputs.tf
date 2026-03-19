output "tailscale_auth_key_name" {
  description = "SSM parameter name for Tailscale auth key"
  value       = aws_ssm_parameter.tailscale_auth_key.name
}

output "tailscale_auth_key_arn" {
  description = "SSM parameter ARN for Tailscale auth key"
  value       = aws_ssm_parameter.tailscale_auth_key.arn
}

output "db_password_name" {
  description = "SSM parameter name for database password"
  value       = aws_ssm_parameter.db_password.name
}

output "db_password_arn" {
  description = "SSM parameter ARN for database password"
  value       = aws_ssm_parameter.db_password.arn
}
