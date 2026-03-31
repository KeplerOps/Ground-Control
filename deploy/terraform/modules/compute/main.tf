# -----------------------------------------------------------------------------
# Compute Module — EC2 instance with IAM profile and EBS data volume
# -----------------------------------------------------------------------------

# --- IAM Instance Profile ---

resource "aws_iam_role" "instance" {
  name = "${var.name_prefix}-ec2"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "ssm_read" {
  name = "ssm-read"
  role = aws_iam_role.instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SSMGetParameters"
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
        ]
        Resource = "arn:aws:ssm:${var.aws_region}:*:parameter/gc/*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "s3_backup" {
  name = "s3-backup"
  role = aws_iam_role.instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3BackupWrite"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
        ]
        Resource = [
          var.backup_bucket_arn,
          "${var.backup_bucket_arn}/*",
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy" "ecr_pull" {
  name = "ecr-pull"
  role = aws_iam_role.instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ECRAuth"
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
        ]
        Resource = "*"
      },
      {
        Sid    = "ECRPull"
        Effect = "Allow"
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchCheckLayerAvailability",
        ]
        Resource = "arn:aws:ecr:${var.aws_region}:*:repository/ground-control"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "instance" {
  name = "${var.name_prefix}-ec2"
  role = aws_iam_role.instance.name
}

# --- Data Volume ---

resource "aws_ebs_volume" "data" {
  availability_zone = var.availability_zone
  size              = var.data_volume_size
  type              = "gp3"
  encrypted         = true

  tags = {
    Name    = "${var.name_prefix}-data"
    Backup  = "true"
    Service = "ground-control"
  }
}

# --- EC2 Instance ---

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

resource "aws_instance" "this" {
  ami                    = var.ami_id != null ? var.ami_id : data.aws_ami.al2023.id
  instance_type          = var.instance_type
  availability_zone      = var.availability_zone
  vpc_security_group_ids = [var.security_group_id]
  iam_instance_profile   = aws_iam_instance_profile.instance.name
  subnet_id              = var.subnet_id

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = base64encode(templatefile("${path.module}/user-data.sh.tftpl", {
    aws_region            = var.aws_region
    data_device           = var.data_device
    ssm_tailscale_key     = var.ssm_tailscale_key
    ssm_db_password       = var.ssm_db_password
    tailscale_hostname    = var.tailscale_hostname
    ecr_registry_url      = var.ecr_registry_url
    backup_bucket         = var.backup_bucket_name
    backup_cron           = var.backup_cron
    local_retention_count = var.local_retention_count
    gc_image              = var.gc_image
    gc_database_user      = var.gc_database_user
    gc_database_name      = var.gc_database_name
    ssm_embedding_api_key = var.ssm_embedding_api_key
    gc_embedding_provider = var.gc_embedding_provider
  }))

  tags = {
    Name = "${var.name_prefix}-ec2"
  }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

resource "aws_volume_attachment" "data" {
  device_name = var.data_device
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.this.id

  # Prevent forced replacement when instance is recreated
  skip_destroy = true
}
