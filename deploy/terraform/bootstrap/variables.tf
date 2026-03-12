variable "aws_region" {
  description = "AWS region for state backend resources"
  type        = string
  default     = "us-east-2"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "groundcontrol"
}

variable "state_bucket_name" {
  description = "Name of the S3 bucket for Terraform state"
  type        = string
  default     = "groundcontrol-terraform-state-catalyst-dev"
}

variable "lock_table_name" {
  description = "Name of the DynamoDB table for state locking"
  type        = string
  default     = "groundcontrol-terraform-lock"
}

variable "github_repo" {
  description = "GitHub repository for OIDC trust policy (org/repo format)"
  type        = string
  default     = "KeplerOps/Ground-Control"
}
