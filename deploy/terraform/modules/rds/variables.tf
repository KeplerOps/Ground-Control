variable "identifier" {
  description = "RDS instance identifier"
  type        = string
  default     = "groundcontrol-dev"
}

variable "engine_version" {
  description = "PostgreSQL major version"
  type        = string
  default     = "16"
}

variable "instance_class" {
  description = "RDS instance type"
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Allocated storage in GiB"
  type        = number
  default     = 20
}

variable "storage_type" {
  description = "EBS volume type"
  type        = string
  default     = "gp3"
}

variable "database_name" {
  description = "Name of the initial database"
  type        = string
  default     = "ground_control"
}

variable "database_user" {
  description = "Master username"
  type        = string
  default     = "gcadmin"
}

variable "port" {
  description = "Database port"
  type        = number
  default     = 5432
}

variable "security_group_id" {
  description = "Security group ID from the networking module"
  type        = string
}

variable "backup_retention_period" {
  description = "Number of days to retain automated backups"
  type        = number
  default     = 7
}

variable "deletion_protection" {
  description = "Prevent accidental deletion of the RDS instance"
  type        = bool
  default     = true
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot on deletion"
  type        = bool
  default     = false
}

variable "final_snapshot_identifier" {
  description = "Name of the final snapshot taken on deletion"
  type        = string
  default     = "groundcontrol-final"
}

variable "multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "auto_minor_version_upgrade" {
  description = "Enable automatic minor version upgrades"
  type        = bool
  default     = true
}

variable "maintenance_window" {
  description = "Preferred maintenance window"
  type        = string
  default     = "sun:06:00-sun:07:00"
}

variable "publicly_accessible" {
  description = "Allow public access (security group restricts actual connectivity)"
  type        = bool
  default     = true
}
