variable "name_prefix" {
  description = "Resource name prefix"
  type        = string
  default     = "groundcontrol"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3a.small"
}

variable "ami_id" {
  description = "AMI ID override. If null, uses latest Amazon Linux 2023."
  type        = string
  default     = null
}

variable "availability_zone" {
  description = "Availability zone for EC2 and EBS volume"
  type        = string
}

variable "subnet_id" {
  description = "Subnet ID for the EC2 instance"
  type        = string
}

variable "security_group_id" {
  description = "Security group ID from the networking module"
  type        = string
}

variable "data_volume_size" {
  description = "EBS data volume size in GiB"
  type        = number
  default     = 20
}

variable "data_device" {
  description = "EBS data volume device name"
  type        = string
  default     = "/dev/xvdf"
}

variable "ssm_tailscale_key" {
  description = "SSM parameter path for Tailscale auth key"
  type        = string
  default     = "/gc/dev/tailscale_auth_key"
}

variable "ssm_db_password" {
  description = "SSM parameter path for database password"
  type        = string
  default     = "/gc/dev/db_password"
}

variable "tailscale_hostname" {
  description = "Tailscale MagicDNS hostname for the instance"
  type        = string
  default     = "gc-dev"
}

variable "backup_bucket_name" {
  description = "S3 bucket name for backups"
  type        = string
}

variable "backup_bucket_arn" {
  description = "S3 bucket ARN for backups (used in IAM policy)"
  type        = string
}

variable "gc_image" {
  description = "Docker image for Ground Control backend"
  type        = string
  default     = "ghcr.io/keplerops/ground-control:latest"
}

variable "gc_database_user" {
  description = "PostgreSQL database username"
  type        = string
  default     = "gc"
}

variable "gc_database_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "ground_control"
}
