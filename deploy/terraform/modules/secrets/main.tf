resource "aws_ssm_parameter" "db_host" {
  name  = "${var.parameter_prefix}/db_host"
  type  = "String"
  value = var.db_host
}

resource "aws_ssm_parameter" "db_username" {
  name  = "${var.parameter_prefix}/db_username"
  type  = "String"
  value = var.db_username
}

resource "aws_ssm_parameter" "db_password" {
  name  = "${var.parameter_prefix}/db_password"
  type  = "SecureString"
  value = var.db_password
}
