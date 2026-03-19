output "instance_id" {
  description = "EC2 instance ID"
  value       = module.compute.instance_id
}

output "private_ip" {
  description = "EC2 private IP address"
  value       = module.compute.private_ip
}

output "tailscale_hostname" {
  description = "Tailscale MagicDNS hostname"
  value       = var.tailscale_hostname
}

output "security_group_id" {
  description = "Instance security group ID"
  value       = module.networking.security_group_id
}

output "data_volume_id" {
  description = "EBS data volume ID"
  value       = module.compute.data_volume_id
}

output "backup_bucket" {
  description = "S3 backup bucket name"
  value       = module.backup.bucket_name
}

output "ssm_tailscale_key" {
  description = "SSM parameter name for Tailscale auth key"
  value       = module.secrets.tailscale_auth_key_name
}

output "ssm_db_password" {
  description = "SSM parameter name for database password"
  value       = module.secrets.db_password_name
}

output "ecr_repository_url" {
  description = "ECR repository URL for Ground Control images"
  value       = aws_ecr_repository.app.repository_url
}
