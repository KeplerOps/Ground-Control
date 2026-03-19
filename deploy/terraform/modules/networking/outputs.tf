output "security_group_id" {
  description = "ID of the instance security group"
  value       = aws_security_group.instance.id
}

output "security_group_name" {
  description = "Name of the instance security group"
  value       = aws_security_group.instance.name
}

output "vpc_id" {
  description = "VPC ID used"
  value       = local.vpc_id
}

output "subnet_ids" {
  description = "Subnet IDs in the VPC"
  value       = data.aws_subnets.default.ids
}
