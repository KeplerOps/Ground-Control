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

# --- Networking (zero-ingress SG, Tailscale-only) ---

module "networking" {
  source = "../../modules/networking"
}

# --- Secrets (SSM parameters) ---

module "secrets" {
  source = "../../modules/secrets"
}

# --- Backup (S3 bucket + DLM snapshots) ---

module "backup" {
  source      = "../../modules/backup"
  bucket_name = var.backup_bucket_name
}

# --- Compute (EC2 + IAM + EBS) ---

module "compute" {
  source = "../../modules/compute"

  aws_region        = var.aws_region
  availability_zone = var.availability_zone
  subnet_id         = var.subnet_id != null ? var.subnet_id : module.networking.subnet_ids[0]
  security_group_id = module.networking.security_group_id

  instance_type      = var.instance_type
  data_volume_size   = var.data_volume_size
  tailscale_hostname = var.tailscale_hostname
  gc_image           = var.gc_image

  backup_bucket_name = module.backup.bucket_name
  backup_bucket_arn  = module.backup.bucket_arn

  ssm_tailscale_key = module.secrets.tailscale_auth_key_name
  ssm_db_password   = module.secrets.db_password_name
}
