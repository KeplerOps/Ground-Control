variable "vpc_id" {
  description = "VPC ID. If null, the default VPC is used."
  type        = string
  default     = null
}

variable "name_prefix" {
  description = "Resource name prefix"
  type        = string
  default     = "groundcontrol"
}
