output "endpoint" {
  description = "RDS endpoint (host:port)"
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "RDS hostname"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "RDS port"
  value       = aws_db_instance.this.port
}

output "database_name" {
  description = "Name of the database"
  value       = aws_db_instance.this.db_name
}

output "database_user" {
  description = "Master username"
  value       = aws_db_instance.this.username
}

output "database_password" {
  description = "Master password"
  value       = random_password.master.result
  sensitive   = true
}

output "instance_arn" {
  description = "ARN of the RDS instance"
  value       = aws_db_instance.this.arn
}

output "parameter_group_id" {
  description = "ID of the DB parameter group"
  value       = aws_db_parameter_group.this.id
}
