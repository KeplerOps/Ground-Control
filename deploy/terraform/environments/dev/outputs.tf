output "rds_endpoint" {
  description = "RDS endpoint (host:port)"
  value       = module.rds.endpoint
}

output "rds_address" {
  description = "RDS hostname"
  value       = module.rds.address
}

output "rds_port" {
  description = "RDS port"
  value       = module.rds.port
}

output "database_name" {
  description = "Database name"
  value       = module.rds.database_name
}

output "security_group_id" {
  description = "Database security group ID"
  value       = module.networking.security_group_id
}

output "ssm_parameter_db_host" {
  description = "SSM parameter name for database host"
  value       = module.secrets.parameter_host_name
}

output "ssm_parameter_db_username" {
  description = "SSM parameter name for database username"
  value       = module.secrets.parameter_username_name
}

output "ssm_parameter_db_password" {
  description = "SSM parameter name for database password"
  value       = module.secrets.parameter_password_name
}
