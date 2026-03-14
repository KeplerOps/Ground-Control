variable "vpc_id" {
  description = "VPC ID. If null, the default VPC is used."
  type        = string
  default     = null
}

variable "allowed_cidr" {
  description = "CIDR block allowed to access the database, e.g. 203.0.113.45/32"
  type        = string
}

variable "port" {
  description = "Database port"
  type        = number
  default     = 5432
}

variable "name_prefix" {
  description = "Resource name prefix"
  type        = string
  default     = "groundcontrol"
}
