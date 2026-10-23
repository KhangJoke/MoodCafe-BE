mock_provider "aws" {
  override_during = plan
  mock_data "aws_caller_identity" { defaults = { account_id = "123456789012" } }
  mock_data "aws_partition" { defaults = { partition = "aws" } }
  mock_data "aws_availability_zones" { defaults = { names = ["ap-southeast-1a"] } }
  mock_data "aws_ssm_parameter" { defaults = { value = "ami-0123456789abcdef0" } }
  mock_resource "aws_ebs_volume" { defaults = { id = "vol-0123456789abcdef0" } }
  mock_resource "aws_ecr_repository" {
    defaults = {
      repository_url = "123456789012.dkr.ecr.ap-southeast-1.amazonaws.com/moodcafe-backend"
      arn            = "arn:aws:ecr:ap-southeast-1:123456789012:repository/moodcafe-backend"
    }
  }
}

run "low_cost_defaults" {
  command = plan
  assert {
    condition     = length(aws_instance.backend.user_data_base64) <= 21844
    error_message = "Compressed user-data must fit the EC2 16 KiB limit."
  }
  assert {
    condition     = aws_instance.backend.instance_type == "t4g.small" && aws_instance.backend.credit_specification[0].cpu_credits == "standard"
    error_message = "Default must be t4g.small with Standard credits."
  }
  assert {
    condition     = aws_instance.backend.metadata_options[0].http_tokens == "required"
    error_message = "IMDSv2 must be required."
  }
  assert {
    condition     = length(aws_vpc_security_group_ingress_rule.ssh) == 0 && toset(keys(aws_vpc_security_group_ingress_rule.web)) == toset(["80", "443"])
    error_message = "Only HTTP/HTTPS ingress is enabled by default."
  }
  assert {
    condition     = aws_ebs_volume.data.encrypted && aws_ebs_volume.data.type == "gp3" && aws_instance.backend.root_block_device[0].encrypted
    error_message = "Both disks must be encrypted gp3 volumes."
  }
  assert {
    condition     = length(aws_s3_bucket.backups) == 1 && aws_s3_bucket_public_access_block.backups[0].block_public_policy
    error_message = "Default PostgreSQL backups must be private."
  }
  assert {
    condition     = startswith(output.api_base_url, "http://")
    error_message = "No domain defaults to an explicitly HTTP endpoint."
  }
}

run "domain_and_optional_backups" {
  command = plan
  variables {
    domain_name    = "api.example.com"
    enable_backups = false
  }
  assert {
    condition     = output.api_base_url == "https://api.example.com/api" && length(aws_s3_bucket.backups) == 0
    error_message = "Domain must produce HTTPS; disabled backups must create no S3 bucket."
  }
}

run "reject_public_ssh" {
  command = plan
  variables { ssh_cidr = "0.0.0.0/0" }
  expect_failures = [var.ssh_cidr]
}

run "require_key_when_ssh_enabled" {
  command = plan
  variables { ssh_cidr = "203.0.113.10/32" }
  expect_failures = [aws_instance.backend]
}
