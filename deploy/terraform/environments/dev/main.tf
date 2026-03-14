provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "ground-control"
      Environment = "dev"
      ManagedBy   = "terraform"
    }
  }
}

module "networking" {
  source       = "../../modules/networking"
  allowed_cidr = var.allowed_cidr
}

module "rds" {
  source            = "../../modules/rds"
  security_group_id = module.networking.security_group_id
}

module "secrets" {
  source      = "../../modules/secrets"
  db_host     = module.rds.address
  db_username = module.rds.database_user
  db_password = module.rds.database_password
}
