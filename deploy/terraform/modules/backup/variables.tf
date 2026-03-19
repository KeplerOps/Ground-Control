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
