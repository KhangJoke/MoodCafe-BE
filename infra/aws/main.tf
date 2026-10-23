data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}
data "aws_availability_zones" "available" { state = "available" }
data "aws_ssm_parameter" "ubuntu_arm64" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/arm64/hvm/ebs-gp3/ami-id"
}

locals {
  az          = var.availability_zone != "" ? var.availability_zone : data.aws_availability_zones.available.names[0]
  backup_name = "${var.project_name}-${data.aws_caller_identity.current.account_id}-${var.aws_region}-backups"
  caddy_site  = var.domain_name == "" ? ":80" : var.domain_name
}

resource "aws_vpc" "main" {
  cidr_block           = "10.42.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "${var.project_name}-vpc" }
}
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.42.1.0/24"
  availability_zone       = local.az
  map_public_ip_on_launch = true
  tags                    = { Name = "${var.project_name}-public" }
}
resource "aws_internet_gateway" "main" { vpc_id = aws_vpc.main.id }
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
}
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "backend" {
  name_prefix = "${var.project_name}-"
  description = "HTTP/HTTPS only; optional administrator SSH; no DB/Redis/app ports"
  vpc_id      = aws_vpc.main.id
}
resource "aws_vpc_security_group_ingress_rule" "web" {
  for_each          = toset(["80", "443"])
  security_group_id = aws_security_group.backend.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = tonumber(each.value)
  to_port           = tonumber(each.value)
}
resource "aws_vpc_security_group_ingress_rule" "ssh" {
  count             = var.ssh_cidr == "" ? 0 : 1
  security_group_id = aws_security_group.backend.id
  cidr_ipv4         = var.ssh_cidr
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
}
resource "aws_vpc_security_group_egress_rule" "outbound" {
  security_group_id = aws_security_group.backend.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
resource "aws_key_pair" "admin" {
  count      = var.ssh_public_key == "" ? 0 : 1
  key_name   = "${var.project_name}-admin"
  public_key = var.ssh_public_key
}

resource "aws_ecr_repository" "backend" {
  name                 = "${var.project_name}-backend"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false
  encryption_configuration { encryption_type = "AES256" }
  image_scanning_configuration { scan_on_push = true }
}

resource "aws_iam_role" "instance" {
  name_prefix = "${var.project_name}-ec2-"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Action = "sts:AssumeRole", Principal = { Service = "ec2.amazonaws.com" } }]
  })
}
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.instance.name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore"
}
resource "aws_iam_role_policy" "runtime" {
  role = aws_iam_role.instance.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat([
      { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
      { Effect = "Allow", Action = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"], Resource = aws_ecr_repository.backend.arn }
      ], var.enable_backups ? [
      { Effect = "Allow", Action = ["s3:PutObject", "s3:GetObject"], Resource = "${aws_s3_bucket.backups[0].arn}/postgres/*" }
    ] : [])
  })
}
resource "aws_iam_instance_profile" "backend" {
  name_prefix = "${var.project_name}-"
  role        = aws_iam_role.instance.name
}

resource "aws_ebs_volume" "data" {
  availability_zone = local.az
  size              = var.data_volume_gb
  type              = "gp3"
  encrypted         = true
  tags              = { Name = "${var.project_name}-data" }
  lifecycle { prevent_destroy = true }
}
resource "aws_instance" "backend" {
  ami                         = nonsensitive(data.aws_ssm_parameter.ubuntu_arm64.value)
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.backend.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.backend.name
  key_name                    = var.ssh_public_key == "" ? null : aws_key_pair.admin[0].key_name
  credit_specification { cpu_credits = "standard" }
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }
  root_block_device {
    volume_size = var.root_volume_gb
    volume_type = "gp3"
    encrypted   = true
  }
  # Secrets are installed manually, never placed in Terraform variables/state/user-data.
  user_data_base64 = base64gzip(replace(templatefile("${path.module}/templates/bootstrap.sh.tftpl", {
    volume_serial   = replace(aws_ebs_volume.data.id, "-", "")
    compose_b64     = base64encode(replace(file("${path.module}/runtime/compose.yml"), "\r\n", "\n"))
    env_b64         = base64encode(replace(file("${path.module}/runtime/.env.example"), "\r\n", "\n"))
    deploy_b64      = base64encode(replace(file("${path.module}/runtime/deploy.sh"), "\r\n", "\n"))
    backup_b64      = base64encode(replace(file("${path.module}/runtime/backup.sh"), "\r\n", "\n"))
    caddy_b64       = base64encode("${local.caddy_site} {\n  encode gzip\n  reverse_proxy backend:8080\n}\n")
    config_b64      = base64encode("AWS_REGION=${var.aws_region}\nECR_REPOSITORY=${aws_ecr_repository.backend.repository_url}\nBACKUP_BUCKET=${var.enable_backups ? aws_s3_bucket.backups[0].bucket : ""}\n")
    backups_enabled = var.enable_backups
  }), "\r\n", "\n"))
  user_data_replace_on_change = true
  tags                        = { Name = "${var.project_name}-backend" }
  depends_on                  = [aws_route_table_association.public, aws_iam_role_policy_attachment.ssm, aws_iam_role_policy.runtime]
  lifecycle {
    # Pin the selected AMI after first launch; refresh it deliberately during maintenance.
    ignore_changes = [ami]
    precondition {
      condition     = var.ssh_cidr == "" || var.ssh_public_key != ""
      error_message = "Mở SSH yêu cầu ssh_public_key. Mặc định dùng Session Manager."
    }
  }
}
resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.backend.id
}
resource "aws_eip" "backend" {
  domain     = "vpc"
  instance   = aws_instance.backend.id
  depends_on = [aws_internet_gateway.main]
}

