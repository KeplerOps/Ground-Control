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

variable "embedding_api_key" {
  description = "API key for the embedding provider (e.g. OpenAI)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "github_token" {
  description = "GitHub personal access token for issue sync and creation"
  type        = string
  sensitive   = true
  default     = ""
}
