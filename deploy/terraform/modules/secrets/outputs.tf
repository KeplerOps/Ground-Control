output "parameter_host_name" {
  description = "SSM parameter name for database host"
  value       = aws_ssm_parameter.db_host.name
}

output "parameter_host_arn" {
  description = "SSM parameter ARN for database host"
  value       = aws_ssm_parameter.db_host.arn
}

output "parameter_username_name" {
  description = "SSM parameter name for database username"
  value       = aws_ssm_parameter.db_username.name
}

output "parameter_username_arn" {
  description = "SSM parameter ARN for database username"
  value       = aws_ssm_parameter.db_username.arn
}

output "parameter_password_name" {
  description = "SSM parameter name for database password"
  value       = aws_ssm_parameter.db_password.name
}

output "parameter_password_arn" {
  description = "SSM parameter ARN for database password"
  value       = aws_ssm_parameter.db_password.arn
}
