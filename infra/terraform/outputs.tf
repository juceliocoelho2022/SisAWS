output "cloudfront_url" {
  description = "Public SisAWS application URL."
  value       = "https://${aws_cloudfront_distribution.app.domain_name}"
}

output "api_url" {
  description = "API URL exposed through CloudFront."
  value       = "https://${aws_cloudfront_distribution.app.domain_name}/api/v1"
}

output "frontend_bucket" {
  description = "S3 bucket for the React dist files."
  value       = aws_s3_bucket.frontend.bucket
}

output "backend_ecr_repository_url" {
  description = "ECR repository for the backend image."
  value       = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  description = "Private RDS PostgreSQL endpoint."
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}

output "backend_deployed" {
  description = "Whether ECS is configured to run backend tasks."
  value       = var.deploy_backend
}
