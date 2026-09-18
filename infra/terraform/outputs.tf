output "frontend_url" {
  description = "Public frontend URL."
  value       = local.frontend_origin
}

output "cloudfront_url" {
  description = "CloudFront URL when enabled."
  value       = var.enable_cloudfront ? "https://${aws_cloudfront_distribution.app[0].domain_name}" : null
}

output "api_url" {
  description = "Public API URL for the frontend build."
  value       = var.enable_cloudfront ? "https://${aws_cloudfront_distribution.app[0].domain_name}/api/v1" : "http://${aws_lb.api.dns_name}/api/v1"
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

output "network_profile" {
  description = "Network profile used by ECS."
  value       = var.use_nat_gateway ? "private-with-nat" : "public-ip-no-nat"
}
