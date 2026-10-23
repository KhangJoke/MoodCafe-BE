output "instance_id" { value = aws_instance.backend.id }
output "public_ip" { value = aws_eip.backend.public_ip }
output "api_base_url" {
  value = var.domain_name == "" ? "http://${aws_eip.backend.public_ip}/api" : "https://${var.domain_name}/api"
}
output "ecr_repository_url" { value = aws_ecr_repository.backend.repository_url }
output "aws_region" { value = var.aws_region }
output "data_volume_id" { value = aws_ebs_volume.data.id }
output "backup_bucket" { value = var.enable_backups ? aws_s3_bucket.backups[0].bucket : null }
output "ssm_command" {
  value = "aws ssm start-session --region ${var.aws_region} --target ${aws_instance.backend.id}"
}

