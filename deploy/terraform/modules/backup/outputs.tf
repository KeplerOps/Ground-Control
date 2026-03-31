output "bucket_name" {
  description = "S3 backup bucket name"
  value       = aws_s3_bucket.backups.id
}

output "bucket_arn" {
  description = "S3 backup bucket ARN"
  value       = aws_s3_bucket.backups.arn
}

output "dlm_policy_id" {
  description = "DLM lifecycle policy ID"
  value       = aws_dlm_lifecycle_policy.snapshots.id
}

output "backup_cron" {
  description = "Cron expression for pg_dump backup schedule"
  value       = var.backup_cron
}

output "local_retention_count" {
  description = "Number of local backup files to retain"
  value       = var.local_retention_count
}
