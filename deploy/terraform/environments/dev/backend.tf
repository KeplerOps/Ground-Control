terraform {
  backend "s3" {
    bucket         = "groundcontrol-terraform-state-catalyst-dev"
    key            = "environments/dev/terraform.tfstate"
    region         = "us-east-2"
    dynamodb_table = "groundcontrol-terraform-lock"
    encrypt        = true
  }
}
