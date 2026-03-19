# -----------------------------------------------------------------------------
# Networking — Zero-ingress security group (Tailscale-only access)
# -----------------------------------------------------------------------------

data "aws_vpc" "default" {
  count   = var.vpc_id == null ? 1 : 0
  default = true
}

locals {
  vpc_id = var.vpc_id != null ? var.vpc_id : data.aws_vpc.default[0].id
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [local.vpc_id]
  }
}

resource "aws_security_group" "instance" {
  name        = "${var.name_prefix}-instance"
  description = "Zero-ingress security group for ${var.name_prefix} — all access via Tailscale"
  vpc_id      = local.vpc_id
}

# No ingress rules — all access is via Tailscale mesh network

resource "aws_security_group_rule" "egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.instance.id
  description       = "Allow all outbound traffic"
}
