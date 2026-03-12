variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "allowed_cidr" {
  description = "Developer IP CIDR for security group ingress"
  type        = string
}
