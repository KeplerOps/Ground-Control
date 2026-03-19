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
