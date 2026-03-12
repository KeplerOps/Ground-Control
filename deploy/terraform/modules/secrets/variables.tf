variable "parameter_prefix" {
  description = "SSM parameter path prefix"
  type        = string
  default     = "/gc/dev"
}

variable "db_host" {
  description = "Database endpoint hostname"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}
