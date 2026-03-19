output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.this.id
}

output "private_ip" {
  description = "EC2 private IP address"
  value       = aws_instance.this.private_ip
}

output "data_volume_id" {
  description = "EBS data volume ID"
  value       = aws_ebs_volume.data.id
}

output "instance_profile_name" {
  description = "IAM instance profile name"
  value       = aws_iam_instance_profile.instance.name
}

output "instance_role_arn" {
  description = "IAM instance role ARN"
  value       = aws_iam_role.instance.arn
}
