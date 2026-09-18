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

output "github_actions_deploy_role_arn" {
  description = "IAM role assumed by GitHub Actions through OIDC for backend deployments."
  value       = aws_iam_role.github_deploy.arn
}

output "github_oidc_provider_arn" {
  description = "GitHub Actions OIDC provider used by the deployment role."
  value       = local.github_oidc_provider_arn
}

output "monthly_budget_name" {
  description = "AWS Budgets monthly cost budget name."
  value       = aws_budgets_budget.monthly.name
}

output "cloudwatch_alarm_names" {
  description = "CloudWatch alarm names created for SisAWS."
  value = [
    aws_cloudwatch_metric_alarm.ecs_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.ecs_memory_high.alarm_name,
    aws_cloudwatch_metric_alarm.alb_unhealthy_targets.alarm_name,
    aws_cloudwatch_metric_alarm.rds_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.rds_free_storage_low.alarm_name
  ]
}

output "alerts_topic_arn" {
  description = "SNS topic ARN used for monitoring notifications when alert_email is configured."
  value       = local.alert_email_enabled ? aws_sns_topic.alerts[0].arn : null
}
