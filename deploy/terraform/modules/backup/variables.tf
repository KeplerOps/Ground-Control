variable "name_prefix" {
  description = "Resource name prefix"
  type        = string
  default     = "groundcontrol"
}

variable "bucket_name" {
  description = "S3 bucket name for database backups"
  type        = string
}

variable "s3_retention_days" {
  description = "Number of days to retain S3 backup objects"
  type        = number
  default     = 30
}

variable "snapshot_retention_count" {
  description = "Number of EBS snapshots to retain"
  type        = number
  default     = 7
}

variable "backup_cron" {
  description = <<-EOT
    Cron expression for automated pg_dump backup. GC-P021 requires at least
    three backups per day; the default (03:00, 11:00, 19:00 UTC) gives an
    approximate 8-hour RPO. Overrides must stay >= 3x/day to remain compliant.
  EOT
  type        = string
  default     = "0 3,11,19 * * *"
}

variable "local_retention_count" {
  description = <<-EOT
    Number of local pg_dump files to retain on the EC2 instance. GC-P021
    requires at least 24 hours of retention; at 3 backups/day the minimum
    compliant value is 4 (>= ~32 hours, one-run margin). Do not lower below 4.
  EOT
  type        = number
  default     = 4
}
