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
  description = "Cron expression for automated pg_dump backup (default: daily at 03:00 UTC)"
  type        = string
  default     = "0 3 * * *"
}

variable "local_retention_count" {
  description = "Number of local pg_dump files to retain on the EC2 instance"
  type        = number
  default     = 3
}
