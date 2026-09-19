resource "aws_s3_bucket" "study_materials" {
  bucket = "${local.name}-study-materials-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "study_materials" {
  bucket = aws_s3_bucket.study_materials.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "study_materials" {
  bucket = aws_s3_bucket.study_materials.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "study_materials" {
  bucket = aws_s3_bucket.study_materials.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "study_materials" {
  bucket = aws_s3_bucket.study_materials.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "study_materials" {
  bucket = aws_s3_bucket.study_materials.id

  rule {
    id     = "cleanup-noncurrent-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_iam_role_policy" "ecs_task_study_materials" {
  name = "${local.name}-study-materials"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid    = "StudyMaterialObjects"
      Effect = "Allow"
      Action = [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ]
      Resource = "${aws_s3_bucket.study_materials.arn}/*"
    }]
  })
}
