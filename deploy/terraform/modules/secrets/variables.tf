variable "parameter_prefix" {
  description = "SSM parameter path prefix"
  type        = string
  default     = "/gc/dev"
}

variable "tailscale_auth_key" {
  description = "Initial Tailscale auth key (will be overwritten manually via AWS CLI)"
  type        = string
  sensitive   = true
  default     = "PLACEHOLDER"
}

variable "db_password" {
  description = "Initial database password (will be overwritten manually via AWS CLI)"
  type        = string
  sensitive   = true
  default     = "PLACEHOLDER"
}
