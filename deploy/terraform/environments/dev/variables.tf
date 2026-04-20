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

variable "backup_cron" {
  description = <<-EOT
    Cron expression for automated pg_dump backup. GC-P021 requires at least
    three backups per day; this env default matches the upstream backup
    module default (`0 3,11,19 * * *`). Overrides must stay >= 3x/day to
    remain compliant — the scripts/assert-backup-policy.sh guardrail fails
    the build otherwise.
  EOT
  type        = string
  default     = "0 3,11,19 * * *"
}

variable "local_retention_count" {
  description = <<-EOT
    Number of local pg_dump files to retain on the EC2 instance. GC-P021
    requires at least 24 hours of retention; at 3 backups/day the minimum
    compliant value is 4. Do not set below 4.
  EOT
  type        = number
  default     = 4
}

variable "gc_embedding_provider" {
  description = "Embedding provider name (openai or none)"
  type        = string
  default     = "none"
}

variable "embedding_api_key" {
  description = "API key for the embedding provider (stored in SSM SecureString)"
  type        = string
  sensitive   = true
  default     = ""
}
