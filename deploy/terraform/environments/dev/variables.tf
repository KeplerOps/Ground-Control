variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "availability_zone" {
  description = "Availability zone for EC2 and EBS"
  type        = string
  default     = "us-east-2a"
}

variable "subnet_id" {
  description = "Subnet ID for EC2 instance. If null, uses first default VPC subnet."
  type        = string
  default     = null
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3a.small"
}

variable "data_volume_size" {
  description = "EBS data volume size in GiB"
  type        = number
  default     = 20
}

variable "tailscale_hostname" {
  description = "Tailscale MagicDNS hostname"
  type        = string
  default     = "gc-dev"
}

variable "backup_bucket_name" {
  description = "S3 bucket name for database backups"
  type        = string
  default     = "groundcontrol-backups-catalyst-dev"
}

variable "embedding_api_key" {
  description = "API key for the embedding provider (stored in SSM SecureString)"
  type        = string
  sensitive   = true
  default     = ""
}
