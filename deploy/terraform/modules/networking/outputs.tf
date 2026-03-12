output "security_group_id" {
  description = "ID of the database security group"
  value       = aws_security_group.db.id
}

output "security_group_name" {
  description = "Name of the database security group"
  value       = aws_security_group.db.name
}
