resource "aws_s3_bucket" "backups" {
  count         = var.enable_backups ? 1 : 0
  bucket        = local.backup_name
  force_destroy = false
}
resource "aws_s3_bucket_public_access_block" "backups" {
  count                   = var.enable_backups ? 1 : 0
  bucket                  = aws_s3_bucket.backups[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  count  = var.enable_backups ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  count  = var.enable_backups ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id
  rule {
    id     = "postgres-retention"
    status = "Enabled"
    filter { prefix = "postgres/" }
    expiration { days = 30 }
    abort_incomplete_multipart_upload { days_after_initiation = 1 }
  }
}
resource "aws_s3_bucket_policy" "backups" {
  count  = var.enable_backups ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Deny", Principal = "*", Action = "s3:*"
      Resource  = [aws_s3_bucket.backups[0].arn, "${aws_s3_bucket.backups[0].arn}/*"]
      Condition = { Bool = { "aws:SecureTransport" = "false" } }
    }]
  })
}

